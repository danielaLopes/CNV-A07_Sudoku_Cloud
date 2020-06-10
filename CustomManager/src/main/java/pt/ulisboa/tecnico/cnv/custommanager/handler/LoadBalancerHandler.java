package pt.ulisboa.tecnico.cnv.custommanager.handler;

import java.util.*;
import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.custommanager.domain.Request;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestState;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RunningInstanceState;
import pt.ulisboa.tecnico.cnv.custommanager.server.LoadBalancerServer;
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
	
	String s = parseRequestBody(t.getRequestBody());
        byte[] bodyBytes = s.getBytes();
	//final InputStream body = t.getRequestBody();
        //byte[] bodyBytes = new byte[body.available()];
        //body.read(bodyBytes);

        // check if requestCost is in cache
        RequestCost cost = LoadBalancerServer.getInstance().getCache().get(query);
        if (cost == null) {
            // check if requestCost is in the dynamoDB, if not estimates the cost
            cost = RequestCostEstimator.getInstance().estimateCost(query);
            _logger.info("Cost estimated: " + cost);
        }
	_logger.info("Cost estimated: " + cost);

        String requestUuid = generateRequestUuid();
        Request request = new Request(cost, query, t, bodyBytes);

       LoadBalancerServer.getInstance().solutionRequest(requestUuid, request);
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

/*public void solutionRequest(String requestUuid, Request request) {

        RunningInstanceState instanceState = selectInstance(request.getCost());
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
            instanceState = selectInstance(request.getCost());
        }
        Instance instance = InstanceSelector.getInstance().getInstanceById(instanceState.getInstanceId());
        _logger.info("Selected instance " + instanceState.getInstanceId());

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
           instanceState.removeRequest(requestUuid);
            solutionRequest(requestUuid, request);
        }

        if (response != null) {

            String fields[] = response.split(":");
            String solution = fields[0];
            Long fieldLoads = Long.parseLong(fields[1]);
            RequestCost actualCost = new RequestCost(fieldLoads);

            // saves requestCost return by server in the cache
//            _cache.put(request.getQuery(), actualCost);

            // Updates the state of the instance that performed this request
            instanceState.updateTotalFieldLoads(fieldLoads);
            instanceState.removeRequest(requestUuid);

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
            instanceState.removeRequest(requestUuid);
            solutionRequest(requestUuid, request);
        }
    }
  */
}
