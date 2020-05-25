package pt.ulisboa.tecnico.cnv.custommanager.server;

import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.custommanager.domain.Request;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RunningInstanceState;
import pt.ulisboa.tecnico.cnv.custommanager.service.*;
import pt.ulisboa.tecnico.cnv.custommanager.handler.LoadBalancerHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LoadBalancerServer {

    private static LoadBalancerServer _instance = null;

    private static Logger _logger = Logger.getLogger(InstanceSelector.class.getName());

    private static RequestCostCache _cache = new RequestCostCache();

    // Delay so that all instances are gathered before starting the autoscaler
    private static final int AUTO_SCALER_DELAY = 0; // minutes
    private static final int AUTO_SCALER_PERIOD = 1; // minutes
    // TODO: change this
    private static final int HEALTH_CHECK_GRACE_PERIOD = 60; // seconds
    private static final int HEALTH_CHECKER_PERIOD = 60; // seconds

    public static LoadBalancerServer getInstance() {
        if (_instance == null) {
            _instance = new LoadBalancerServer();
        }
        return _instance;
    }

    private LoadBalancerServer() {}

    public RequestCostCache getCache() { return _cache; }

    public static void main(final String[] args) throws Exception {

        //final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8000), 0);
        final HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        // main handler that receives requests from sudoku solver website
        server.createContext("/sudoku", new LoadBalancerHandler());
        // handler that receives responses to requests from WebServer instances
        //server.createContext("/response", new ResponseHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        gatherAllInstancesTest();

        // schedules AutoScaler to execute repeatedly every check period of 1 minute
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        // TODO: keep in mind that the period to shut down a machine is 5 and not 1
        scheduler.scheduleAtFixedRate(new AutoScaler(), AUTO_SCALER_DELAY, AUTO_SCALER_PERIOD, TimeUnit.MINUTES);

        // schedules Healthcheck to execute repeatedly every check period of 300 seconds
        // TODO: do i need to create a new schedular ?
        //ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        // TODO: change period to 5
        scheduler.scheduleAtFixedRate(new HealthChecker(), HEALTH_CHECK_GRACE_PERIOD, HEALTH_CHECKER_PERIOD, TimeUnit.SECONDS);

        // shutdown everything when LoadBalancer goes down
        //Runtime.getRuntime().addShutdownHook(new Shutdown());

        System.out.println(server.getAddress().toString());

        //orderInstancesTest();

        //gatherAllInstancesTest();
        //startInstanceTest();
        //Thread.sleep(1000); // sleeps for 10 seconds
        //shutdownInstanceTest();
        //terminateInstanceTest();
        //terminateAllInstancesTest();
    }

    /**
     * Perform instance cleanup: terminates instances in all reservations
     */
    static class Shutdown extends Thread {
        @Override
        public void run() {
            InstanceSelector.getInstance().shutdown();
        }
    }

    /**
     * When a machine fails, all the requests it was processing are redirectioned to other machines
     */
    public static void repeatProcessingRequests(Map<String, Request> processingRequests) {

        for (final Map.Entry<String, Request> request: processingRequests.entrySet()) {
            Thread thread = new Thread(){
                public void run(){
                    solutionRequest(request.getKey(), request.getValue());
                }
            };
            thread.start();
        }
    }

    public static void solutionRequest(String requestUuid, Request request) {

        RunningInstanceState instanceState = InstanceSelector.getInstance().selectInstance(request.getCost());
        if (instanceState == null) {
            // TODO: see if this can be problematic
            // waits until new instances are initialized
            while (InstanceSelector.getInstance().instancesInitializing() == true) {
                _logger.info("Waiting for new instances to be initialized!");
                try {
                    Thread.sleep(10000); // 10 seconds
                }
                catch(InterruptedException e) {
                    _logger.warning("Failed to wait for instances to initialize");
                }
            }
            instanceState = InstanceSelector.getInstance().selectInstance(request.getCost());
        }
        Instance instance = InstanceSelector.getInstance().getInstanceById(instanceState.getInstanceId());

        // Sends request for chosen instance to solve sudoku
        instanceState.addNewRequest(requestUuid, request);

        String response = null;
        try {
            response = SendMessages.getInstance().sendSudokuRequest(
                    instance, request.getQuery(), request.getBody(), request.getCost().getTimeEstimation());
        }
        catch(IOException e) {
            _logger.warning(e + " Request could not be processed by instance " +
                    instanceState.getInstanceId() + " . Repeating it...");
            solutionRequest(requestUuid, request);
        }

        if (response != null) {

            String fields[] = response.split(":");
            String solution = fields[0];
            Long fieldLoads = Long.parseLong(fields[1]);
            RequestCost actualCost = new RequestCost(fieldLoads);

            // saves requestCost return by server in the cache
            _cache.put(request.getQuery(), actualCost);

            _logger.info("after requestCostCache");

            // Updates the state of the instance that performed this request
            instanceState.updateTotalFieldLoads(fieldLoads);
            instanceState.removeRequest(requestUuid);

            _logger.info("solution " + solution);
            _logger.info("field loads " + fieldLoads);

            try {
                SendMessages.getInstance().sendClientResponse(request.getClientCommunication(), solution);
                _logger.info("Sent response to client.");
            }
            catch(IOException e) {
                _logger.warning("Could not send response to client.");
            }
        }
        else {
            _logger.warning("Request could not be processed by instance " +
                    instanceState.getInstanceId() + " . Repeating it...");
            solutionRequest(requestUuid, request);
        }
    }

    public static void gatherAllInstancesTest() { InstanceSelector.getInstance().gatherAllInstances(); }

    public static void startInstanceTest() {
        InstanceSelector.getInstance().startInstances(1);
    }

    /*public static void shutdownInstanceTest() {
        InstanceSelector.getInstance().shutdownInstances(1);
    }*/

    public static void terminateInstanceTest() {
        InstanceSelector.getInstance().terminateInstances(1);
    }

    public static void terminateAllInstancesTest() {
        InstanceSelector.getInstance().terminateAllInstances();
    }

    public static void orderInstancesTest() {
        List<RunningInstanceState> states = new ArrayList<>();
        RunningInstanceState instance1 = new RunningInstanceState("instance1");
        instance1.addNewRequest("request1",
                new Request(new RequestCost(3000L), null, null, null));
        RunningInstanceState instance2 = new RunningInstanceState("instance2");
        instance2.addNewRequest("request2",
                new Request(new RequestCost(1000L), null, null, null));
        RunningInstanceState instance3 = new RunningInstanceState("instance3");
        instance3.addNewRequest("request3",
                new Request(new RequestCost(500L), null, null, null));
        instance3.addNewRequest("request4",
                new Request(new RequestCost(500L), null, null, null));

        states.add(instance1);
        states.add(instance2);
        states.add(instance3);

        instance1.updateTotalFieldLoads(3000L);
        instance2.updateTotalFieldLoads(2000L);
        instance3.updateTotalFieldLoads(1000L);

        Collections.sort(states, RunningInstanceState.LEAST_LATEST_FIELD_LOADS_COMPARATOR);
        _logger.info("LEAST_LATEST_FIELD_LOADS_COMPARATOR");
        for (RunningInstanceState state : states) {
            _logger.info(state.getInstanceId() + ":" + state.getLatestFieldLoads());
        }
        _logger.info("Chosen instance to perform a request: " + states.get(states.size() - 1).getInstanceId());

        // selectInstance()
        Collections.sort(states, RunningInstanceState.LEAST_CPU_AVAILABLE_COMPARATOR);
        _logger.info("LEAST_CPU_AVAILABLE_COMPARATOR");
        for (RunningInstanceState state : states) {
            _logger.info(state.getInstanceId() + ":" + state.getTotalCpuAvailable());
        }
        _logger.info("Chosen instance to perform a request: " + states.get(states.size() - 1).getInstanceId());

        Collections.sort(states, RunningInstanceState.LEAST_SUM_PROCESSING_FIELD_LOADS_COMPARATOR);
        _logger.info("LEAST_SUM_PROCESSING_FIELD_LOADS_COMPARATOR");
        for (RunningInstanceState state : states) {
            _logger.info(state.getInstanceId() + ":" + state.calculateRequestCostSum());
        }



    }

    public static void orderInstancesByFieldLoads() {

    }
}
