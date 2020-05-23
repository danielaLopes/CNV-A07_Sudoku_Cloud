package pt.ulisboa.tecnico.cnv.custommanager.domain;

import pt.ulisboa.tecnico.cnv.custommanager.handler.LoadBalancerHandler;
import pt.ulisboa.tecnico.cnv.custommanager.service.InstanceSelector;

import java.util.Comparator;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class RunningInstanceState {

    private String _instanceId;
    // Keeps track of all requests currently being processed by this instance
    // Maps each request to the corresponding request cost
    // TODO: does it require to be concurrent
    private ConcurrentHashMap<String, RequestCost> _processingRequests = new ConcurrentHashMap<>();
    // TODO: put as atomic
    private int _estimatedCpu;

    // TODO: how to obtain metrics for each machine on a certain interval?
    private Long _totalFieldLoads;
    // keeps track of all the fieldLoads performed since the latest AutoScaler run
    private Long _latestFieldLoads;
    // TODO: see what amount of field loads correspond to under 20% cpu
    private final Long IDLE_FIELD_LOADS = 1000L;
    private final Long OVERLOADED_FIELD_LOADS = 10000L;

    private boolean _shuttingDown;
    private ScheduledFuture<?> _scheduledShutdownFuture;
    private final int CHECK_SHUTDOWN_PERIOD = 30; // seconds

    private static Logger _logger = Logger.getLogger(RunningInstanceState.class.getName());

    public RunningInstanceState(String instanceId) {
        _instanceId = instanceId;
        _estimatedCpu = 0;
        _shuttingDown = false;
        _totalFieldLoads = 0L;
        _latestFieldLoads = 0L;
    }

    public String getInstanceId() {
        return _instanceId;
    }

    public boolean shuttingDown() { return _shuttingDown; }

    public boolean isIdle() { return _latestFieldLoads <= IDLE_FIELD_LOADS; }

    public boolean isOverloaded() { return _latestFieldLoads > OVERLOADED_FIELD_LOADS; }

    // should be performed at the end of receiving response to each request
    public void updateTotalFieldLoads(Long fieldLoads) {
        _totalFieldLoads += fieldLoads;
        _latestFieldLoads += fieldLoads;
    }

    public Long getLatestFieldLoads() { return _latestFieldLoads; }

    public void resetFieldLoads() {
        _latestFieldLoads = 0L;
    }

    // TODO: query or uuid?
    public void addNewRequest(String query, RequestCost cost) {

        _processingRequests.put(query, cost);
        _estimatedCpu += cost.getCpu();
    }

    public void removeRequest(String query) {

        _estimatedCpu -= _processingRequests.get(query).getCpu();
        _processingRequests.remove(query);

    }


    public Long calculateRequestCostSum() {
        Long totalEstimatedCost = 0L;
        for (RequestCost cost : _processingRequests.values()) {
            totalEstimatedCost += cost.getFieldLoads();
        }
        return totalEstimatedCost;
    }

    // TODO: see what to do with estimated cpu
    public int getTotalCpuOccupied() {
        /*int totalCpu
        for (RequestCost cost : _processingRequests.values()) {
            totalEstimatedCost += cost.getFieldLoads();
        }
        return totalEstimatedCost;*/
        return 0;
    }

    // TODO
    public int getTotalCpuAvailable() { return 0; }

    public static final Comparator<RunningInstanceState> LEAST_CPU_AVAILABLE_COMPARATOR =
            new Comparator<RunningInstanceState>() {
        @Override
        public int compare(RunningInstanceState o1, RunningInstanceState o2) {
            return Integer.compare(o1.getTotalCpuAvailable(), o2.getTotalCpuAvailable());
        }
    };

    public static final Comparator<RunningInstanceState> LEAST_TOTAL_FIELD_LOADS_COMPARATOR =
            new Comparator<RunningInstanceState>() {
        @Override
        public int compare(RunningInstanceState o1, RunningInstanceState o2) {
            return o1.getLatestFieldLoads().compareTo(o2.getLatestFieldLoads());
        }
    };

    public void scheduleShutdown() {
        _shuttingDown = true;
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        _scheduledShutdownFuture = scheduler.scheduleAtFixedRate(new ShutdownChecker(), 0,
                CHECK_SHUTDOWN_PERIOD, TimeUnit.SECONDS);
    }

    public class ShutdownChecker implements Runnable {
        @Override
        public void run() {
            _logger.info("Checking if it is possible to shutdown instance " + _instanceId);
            // instance only shutdowns when there are no requests to fulfill
            if (_processingRequests.size() == 0) {
                _scheduledShutdownFuture.cancel(false);
                InstanceSelector.getInstance().terminateInstance(_instanceId);
            }
        }
    }
}
