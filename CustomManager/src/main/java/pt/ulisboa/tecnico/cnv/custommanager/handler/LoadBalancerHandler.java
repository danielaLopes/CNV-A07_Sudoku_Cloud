package pt.ulisboa.tecnico.cnv.custommanager.handler;

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

        Request request = new Request(cost, query, t, bodyBytes);

        LoadBalancerServer.getInstance().solutionRequest(requestUuid, request);
    }

    public String generateRequestUuid() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
}