package pt.ulisboa.tecnico.cnv.loadbalancer.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import pt.ulisboa.tecnico.cnv.loadbalancer.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.loadbalancer.service.InstanceSelector;
import pt.ulisboa.tecnico.cnv.loadbalancer.service.RequestCostEstimator;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
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
    }

    static class LoadBalancerHandler implements HttpHandler {

        @Override
        public void handle(final HttpExchange t) throws IOException {

            // Get the query.
            final String query = t.getRequestURI().getQuery();
            System.out.println("> Query:\t" + query);

            // Break it down into String[].
            final String[] params = query.split("&");

            final ArrayList<String> newArgs = new ArrayList<>();

            // Store as if it was a direct call to SolverMain.
            for (final String p : params) {
                final String[] splitParam = p.split("=");
                newArgs.add("-" + splitParam[0]);
                newArgs.add(splitParam[1]);
            }

            byte[] solution;

            solution = "ola".getBytes();

            /*if (cachedResponses.contains(query)) {
                solution = cachedResponses.get(query);
            }
            else {*/
                // Estimate request cost
                // RequestCostEstimator contains pre loaded data to help estimate the cost of a request
                RequestCost cost = RequestCostEstimator.estimateCost(query);
                // Choose instance to solve sudoku based on the estimated cost of the request
                // default is choosing always instance with less CPU -> this leads to more machines running at less CPU
                // should we estimate %CPU based on the cost of a request
                //solution = InstanceSelector.getInstance().selectInstance(cost).solveSudoku();
                InstanceSelector.getInstance().selectInstance(cost);
                // Sends request for chosen instance to solve sudoku
                // Do this asynchronously ?? timeout??
                //solution = instance.solveSudoku();
            //}


            // Send response to browser.
            final Headers hdrs = t.getResponseHeaders();

            //t.sendResponseHeaders(200, responseFile.length());

            ///hdrs.add("Content-Type", "image/png");
            hdrs.add("Content-Type", "application/json");

            hdrs.add("Access-Control-Allow-Origin", "*");

            hdrs.add("Access-Control-Allow-Credentials", "true");
            hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
            hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

            t.sendResponseHeaders(200, solution.toString().length());

            final OutputStream os = t.getResponseBody();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(solution.toString());
            osw.flush();
            osw.close();

            os.close();

            System.out.println("> Sent response to " + t.getRemoteAddress().toString());
        }
    }
}
