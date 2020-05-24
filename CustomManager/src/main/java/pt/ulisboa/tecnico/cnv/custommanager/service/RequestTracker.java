package pt.ulisboa.tecnico.cnv.custommanager.service;

import com.sun.net.httpserver.HttpExchange;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestState;
import pt.ulisboa.tecnico.cnv.custommanager.server.LoadBalancerServer;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Keeps track of the requests being processed and sweeps the
 * map every SWEEP_PERIOD to check if there are requests
 * that need to be repeated by other machines due to machine
 * failure or delays.
 * When servers deliver the solution to the LoadBalancer, the
 * request is removed from the map.
 * Every request is uniquely identified
 */
public class RequestTracker {
    // TODO: not currently being used
    private static RequestTracker _instance = null;

    private Logger _logger = Logger.getLogger(InstanceSelector.class.getName());

    private static final int SWEEP_PERIOD = 5000; // in milliseconds

    private final ConcurrentHashMap<String, RequestState> _processingRequests = new ConcurrentHashMap<>();

    private RequestTracker() { init(); }

    public void init() {
        Thread t = new PeriodicSweeper();
        t.start();
    }

    public static RequestTracker getInstance() {
        if (_instance == null) {
            _instance = new RequestTracker();
        }
        return _instance;
    }

    public void add(String key, RequestState value) {

        String instanceId = value.getInstanceId();
        _processingRequests.put(key, value);
        //InstanceSelector.getInstance().getRunningInstanceState(instanceId).addNewRequest(key);
    }

    public void remove(String key) {

        RequestState value = _processingRequests.get(key);
        String instanceId = value.getInstanceId();
        _processingRequests.remove(key);
        //InstanceSelector.getInstance().getRunningInstanceState(instanceId).removeRequest(key);
    }

    public RequestState get(String key) {
        return _processingRequests.get(key);
    }

    class PeriodicSweeper extends Thread {
        @Override
        public void run() {
            while(true) {
                try {
                    Thread.sleep(SWEEP_PERIOD);
                }
                catch(InterruptedException e) {
                    _logger.warning(e.getMessage());
                }
                //sweepRequests();
            }
        }

        /*public void sweepRequests() {
            Iterator<Map.Entry<String, RequestState>> iter = _processingRequests.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, RequestState> r = iter.next();
                r.getValue().getAndIncrementNSweeps();
                if (r.getValue().timeExceeded(SWEEP_PERIOD)) {
                    _logger.info("Time exceeded for request: " + r.getValue().getQuery());
                    HttpExchange clientCommunication = r.getValue().getClientCommunication();
                    iter.remove(); // removes request because it's going to be added again
                    LoadBalancerServer.repeatRequest(clientCommunication);
                }
            }
        }*/
    }
}
