package pt.ulisboa.tecnico.cnv.custommanager.server;

import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.service.InstanceSelector;
import pt.ulisboa.tecnico.cnv.custommanager.handler.LoadBalancerHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class LoadBalancerServer {

    //public ConcurrentMap<String, CostEstimation> costEstimations = new ConcurrentHashMap();
    //public ConcurrentMap<String, JSONArray> cachedResponses = new ConcurrentHashMap<>();


    public static void main(final String[] args) throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);

        server.createContext("/sudoku", new LoadBalancerHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println(server.getAddress().toString());

        gatherAllInstancesTest();
        //startInstanceTest();
        //Thread.sleep(1000); // sleeps for 10 seconds
        //shutdownInstanceTest();
        //terminateInstanceTest();
        //terminateAllInstancesTest();
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
