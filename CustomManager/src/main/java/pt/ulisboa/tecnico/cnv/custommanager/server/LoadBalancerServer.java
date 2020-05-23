package pt.ulisboa.tecnico.cnv.custommanager.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestState;
import pt.ulisboa.tecnico.cnv.custommanager.handler.ResponseHandler;
import pt.ulisboa.tecnico.cnv.custommanager.service.*;
import pt.ulisboa.tecnico.cnv.custommanager.handler.LoadBalancerHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class LoadBalancerServer {

    private static LoadBalancerServer _instance = null;

    private static Logger _logger = Logger.getLogger(InstanceSelector.class.getName());

    // Delay so that all instances are gathered before starting the autoscaler
    private static final int AUTO_SCALER_DELAY = 1; // minutes
    private static final int AUTO_SCALER_PERIOD = 1; // minutes
    // TODO: change this
    private static final int HEALTH_CHECK_GRACE_PERIOD = 3; // seconds
    private static final int HEALTH_CHECKER_PERIOD = 500; // seconds

    public static LoadBalancerServer getInstance() {
        if (_instance == null) {
            _instance = new LoadBalancerServer();
        }
        return _instance;
    }

    private LoadBalancerServer() {}

    public static void main(final String[] args) throws Exception {

        final HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        // main handler that receives requests from sudoku solver website
        server.createContext("/sudoku", new LoadBalancerHandler());
        // handler that receives responses to requests from WebServer instances
        //server.createContext("/response", new ResponseHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        // schedules AutoScaler to execute repeatedly every check period of 1 minute
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        // TODO: keep in mind that the period to shut down a machine is 5 and not 1
        scheduler.scheduleAtFixedRate(new AutoScaler(), AUTO_SCALER_DELAY, AUTO_SCALER_PERIOD, TimeUnit.MINUTES);

        // schedules Healthcheck to execute repeatedly every check period of 300 seconds
        // TODO: do i need to create a new schedular ?
        //ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        // TODO: change period to 5
        scheduler.scheduleAtFixedRate(new HealthChecker(), HEALTH_CHECK_GRACE_PERIOD, 30, TimeUnit.SECONDS);

        // shutdown everything when LoadBalancer goes down
        //Runtime.getRuntime().addShutdownHook(new Shutdown());

        System.out.println(server.getAddress().toString());

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

    public static void repeatRequest(final HttpExchange t) {

        _logger.info("Repeating request that exceeded time");

        // Get the query.
        final String query = t.getRequestURI().getQuery();
        _logger.info("> Query:\t" + query);

        String solution = null;

        //solution = "ola".getBytes();

        // check if request is in cache
        //solution = RequestCostCache.getInstance().get(query);
        if (solution != null) {
            try {
                SendMessages.getInstance().sendClientResponse(t, solution);
            }
            catch(IOException e) {
                _logger.warning(e.getMessage());
            }
        }
        // TODO: call WebServer to start processing request
        else {
            /*// Estimate request cost
            // RequestCostEstimator contains pre loaded data to help estimate the cost of a request
            RequestCost cost = RequestCostEstimator.estimateCost(query);
            // Choose instance to solve sudoku based on the estimated cost of the request
            // default is choosing always instance with less CPU -> this leads to more machines running at less CPU
            // should we estimate %CPU based on the cost of a request
            //solution = InstanceSelector.getInstance().selectInstance(cost).solveSudoku();
            InstanceSelector.getInstance().selectInstance(cost);
            // Sends request for chosen instance to solve sudoku
            // Do this asynchronously ?? timeout??
            //solution = instance.solveSudoku();*/
            //String requestUuid = generateRequestUuid();
            String requestUuid = "uuid";
            // TODO: these are dummy values
            String instanceId = "instance x";
            int expectedTime = 3000;
            RequestState requestState = new RequestState(t, query, instanceId, expectedTime);
            // TODO: see what's best: have a unique id for each request
            // TODO: or map it with the query so that we don't repeat equivalent
            // TODO: requests and avoid overloading servers
            // TODO: use query without the algorithm, since the result is the same ?????
            RequestTracker.getInstance().add(requestUuid, requestState);
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

    public static void orderInstancesByCpu() {

    }

    public static void orderInstancesByFieldLoads() {

    }
}
