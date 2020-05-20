package pt.ulisboa.tecnico.cnv.custommanager.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestState;
import pt.ulisboa.tecnico.cnv.custommanager.handler.ResponseHandler;
import pt.ulisboa.tecnico.cnv.custommanager.service.InstanceSelector;
import pt.ulisboa.tecnico.cnv.custommanager.handler.LoadBalancerHandler;
import pt.ulisboa.tecnico.cnv.custommanager.service.RecentRequestsCache;
import pt.ulisboa.tecnico.cnv.custommanager.service.RequestTracker;
import pt.ulisboa.tecnico.cnv.custommanager.service.SendResponses;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class LoadBalancerServer {

    private static LoadBalancerServer _instance = null;

    private static Logger _logger = Logger.getLogger(InstanceSelector.class.getName());

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
        server.createContext("/response", new ResponseHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();


        /*final HttpServer responsesServer = HttpServer.create(new InetSocketAddress(81), 0);
        responsesServer.createContext("/response", new ResponseHandler());
        responsesServer.setExecutor(Executors.newCachedThreadPool());
        responsesServer.start();*/

        System.out.println(server.getAddress().toString());

        gatherAllInstancesTest();
        //startInstanceTest();
        //Thread.sleep(1000); // sleeps for 10 seconds
        //shutdownInstanceTest();
        //terminateInstanceTest();
        //terminateAllInstancesTest();
    }

    public static void repeatRequest(final HttpExchange t) {

        _logger.info("Repeating request that exceeded time");

        // Get the query.
        final String query = t.getRequestURI().getQuery();
        _logger.info("> Query:\t" + query);

        byte[] solution;

        solution = "ola".getBytes();

        // check if request is in cache
        solution = RecentRequestsCache.getInstance().get(query);
        if (solution != null) {
            try {
                SendResponses.getInstance().sendClientResponse(t, solution);
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
        InstanceSelector.getInstance().startInstances(2);
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


}
