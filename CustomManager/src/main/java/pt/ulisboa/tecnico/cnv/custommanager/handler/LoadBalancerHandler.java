package pt.ulisboa.tecnico.cnv.custommanager.handler;

import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestState;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RunningInstanceState;
import pt.ulisboa.tecnico.cnv.custommanager.service.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

public class LoadBalancerHandler implements HttpHandler {

    private static Logger _logger = Logger.getLogger(LoadBalancerHandler.class.getName());

    @Override
    public void handle(final HttpExchange t) throws IOException {

        // Get the query.
        final String query = t.getRequestURI().getQuery();
        _logger.info("> Query:\t" + query);

        final InputStream body = t.getRequestBody();
        byte[] bodyBytes = new byte[body.available()];
        body.read(bodyBytes);

        // check if requestCost is in cache
        RequestCost cost = RequestCostCache.getInstance().get(query);
        if (cost == null) {
            // check if requestCost is in the dynamoDB, if not estimates the cost
            cost = RequestCostEstimator.getInstance().estimateCost(query);
        }

        String requestUuid = generateRequestUuid();

        // Choose instance to solve sudoku based on the estimated cost of the request
        // default is choosing always instance with less CPU -> this leads to more machines running at less CPU
        // should we estimate %CPU based on the cost of a request
        //solution = InstanceSelector.getInstance().selectInstance(cost).solveSudoku();
        // TODO: select best instance
        RunningInstanceState instanceState = InstanceSelector.getInstance().selectInstance(cost);
        Instance instance = InstanceSelector.getInstance().getInstanceById(instanceState.getInstanceId());

        // Sends request for chosen instance to solve sudoku
        instanceState.addNewRequest(requestUuid, cost);


        // TODO: these are dummy values
        //int expectedTime = 3000;
        //RequestState requestState = new RequestState(t, query, instanceId, expectedTime);
        // TODO: see what's best: have a unique id for each request
        // TODO: or map it with the query so that we don't repeat equivalent
        // TODO: requests and avoid overloading servers
        // TODO: use query without the algorithm, since the result is the same ?????
        //RequestTracker.getInstance().add(requestUuid, requestState);
        String response = SendMessages.getInstance().sendSudokuRequest(instance, query, bodyBytes);

        if (response != null) {
            String fields[] = response.split(":");
            String solution = fields[0];
            Long fieldLoads = Long.parseLong(fields[1]);
            RequestCost actualCost = new RequestCost(fieldLoads);

            // saves requestCost return by server in the cache
            RequestCostCache.getInstance().put(query, actualCost);

            // Updates the state of the instance that performed this request
            instanceState.updateTotalFieldLoads(fieldLoads);
            instanceState.removeRequest(requestUuid);
            // TODO: timeouts and check if request needs to be repeated
            SendMessages.getInstance().sendClientResponse(t, solution);
        }
        else {
            _logger.info("Request could not be processed by instance. Repeating it...");
            handle(t);
        }
    }

    public String generateRequestUuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public void parseServerResponse(String response) {

    }
}