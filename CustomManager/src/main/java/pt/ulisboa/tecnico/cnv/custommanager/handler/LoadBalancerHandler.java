package pt.ulisboa.tecnico.cnv.custommanager.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestState;
import pt.ulisboa.tecnico.cnv.custommanager.service.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

public class LoadBalancerHandler implements HttpHandler {

    private static Logger logger = Logger.getLogger(LoadBalancerHandler.class.getName());

    @Override
    public void handle(final HttpExchange t) throws IOException {

        logger.info("Handling new request");

        // Get the query.
        final String query = t.getRequestURI().getQuery();
        logger.info("> Query:\t" + query);

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

        // check if request is in cache
        solution = RecentRequestsCache.getInstance().get(query);
        if (solution != null) {
            SendResponses.getInstance().sendClientResponse(t, solution);
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
            String requestUuid = generateRequestUuid();
            RequestState requestState = new RequestState();
            // TODO: see what's best: have a unique id for each request
            // TODO: or map it with the query so that we don't repeat equivalent
            // TODO: requests and avoid overloading servers
            // TODO: use query without the algorithm, since the result is the same ?????
            RequestTracker.getInstance().add(requestUuid, requestState);
        }
    }

    public String generateRequestUuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}