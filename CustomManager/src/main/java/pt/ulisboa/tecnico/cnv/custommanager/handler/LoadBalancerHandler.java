package pt.ulisboa.tecnico.cnv.custommanager.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestState;
import pt.ulisboa.tecnico.cnv.custommanager.service.*;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

public class LoadBalancerHandler implements HttpHandler {

    private static Logger _logger = Logger.getLogger(LoadBalancerHandler.class.getName());

    @Override
    public void handle(final HttpExchange t) throws IOException {

        _logger.info("Handling new request");

        // Get the query.
        final String query = t.getRequestURI().getQuery();
        _logger.info("> Query:\t" + query);

        byte[] solution;

        solution = "ola".getBytes();

        // check if request is in cache
        solution = RecentRequestsCache.getInstance().get(query);
        if (solution != null) {
            SendMessages.getInstance().sendClientResponse(t, solution);
        }
        // TODO: call WebServer to start processing request
        else {
            // Estimate request cost
            // RequestCostEstimator contains pre loaded data to help estimate the cost of a request
            RequestCostEstimator costEstimator = RequestCostEstimator.getInstance();
            RequestCost cost = costEstimator.estimateCost(query);
            // Choose instance to solve sudoku based on the estimated cost of the request
            // default is choosing always instance with less CPU -> this leads to more machines running at less CPU
            // should we estimate %CPU based on the cost of a request
            //solution = InstanceSelector.getInstance().selectInstance(cost).solveSudoku();
            InstanceSelector.getInstance().selectInstance(cost);
            // Sends request for chosen instance to solve sudoku
            // Do this asynchronously ?? timeout??
            //solution = instance.solveSudoku();*/
            String requestUuid = generateRequestUuid();
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

    public String generateRequestUuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}