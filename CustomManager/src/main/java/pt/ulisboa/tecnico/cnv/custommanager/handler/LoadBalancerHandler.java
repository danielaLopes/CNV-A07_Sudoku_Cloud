package pt.ulisboa.tecnico.cnv.custommanager.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestState;
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

        _logger.info("Handling new request");

        // Get the query.
        final String query = t.getRequestURI().getQuery();
        _logger.info("> Query:\t" + query);

        final String method = t.getRequestMethod();
        _logger.info("METHOD: " + method);
        final InputStream body = t.getRequestBody();
        byte[] bodyBytes = new byte[body.available()];
        body.read(bodyBytes);

        String parsedBody = parseRequestBody(body);
        System.out.println("parsed body: " + parsedBody);

        System.out.println("body: " + body);

        System.out.println("bodyBytes: " + Arrays.toString(bodyBytes));


        byte[] solution = null;

        //solution = "ola".getBytes();

        // check if request is in cache
        //solution = RecentRequestsCache.getInstance().get(query);
        //if (solution != null) {
            //SendMessages.getInstance().sendClientResponse(t, solution);
        //}
        // TODO: call WebServer to start processing request
        //else {
            // Estimate request cost
            // RequestCostEstimator contains pre loaded data to help estimate the cost of a request
            //RequestCostEstimator costEstimator = RequestCostEstimator.getInstance();
            //RequestCost cost = costEstimator.estimateCost(query);
            // Choose instance to solve sudoku based on the estimated cost of the request
            // default is choosing always instance with less CPU -> this leads to more machines running at less CPU
            // should we estimate %CPU based on the cost of a request
            //solution = InstanceSelector.getInstance().selectInstance(cost).solveSudoku();
            //InstanceSelector.getInstance().selectInstance(cost);
            // Sends request for chosen instance to solve sudoku
            // Do this asynchronously ?? timeout??
            //solution = instance.solveSudoku();*/
            //String requestUuid = generateRequestUuid();
            // TODO: these are dummy values
            //String instanceId = "instance x";
            //int expectedTime = 3000;
            //RequestState requestState = new RequestState(t, query, instanceId, expectedTime);
            // TODO: see what's best: have a unique id for each request
            // TODO: or map it with the query so that we don't repeat equivalent
            // TODO: requests and avoid overloading servers
            // TODO: use query without the algorithm, since the result is the same ?????
            //RequestTracker.getInstance().add(requestUuid, requestState);
            _logger.info("Sending request to a server");
            solution = SendMessages.getInstance().sendLocalSudokuRequest(query, bodyBytes);
            _logger.info("Received solution from server: " + solution);
            SendMessages.getInstance().sendClientResponse(t, solution);
        //}
    }

    public String generateRequestUuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }

    public static String parseRequestBody(InputStream is) throws IOException {
        InputStreamReader isr =  new InputStreamReader(is,"utf-8");
        BufferedReader br = new BufferedReader(isr);

        // From now on, the right way of moving from bytes to utf-8 characters:

        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
            buf.append((char) b);

        }

        br.close();
        isr.close();

        return buf.toString();
    }
}