package pt.ulisboa.tecnico.cnv.custommanager.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import pt.ulisboa.tecnico.cnv.custommanager.service.RequestCostCache;
import pt.ulisboa.tecnico.cnv.custommanager.service.RequestTracker;

import java.io.IOException;
import java.util.logging.Logger;

public class ResponseHandler implements HttpHandler {

    private static Logger logger = Logger.getLogger(ResponseHandler.class.getName());

    // TODO: this might not be needed
    @Override
    public void handle(final HttpExchange t) throws IOException {

        /*logger.info("Handling response to request");

        // Get the query.
        final String query = t.getRequestURI().getQuery();

        byte[] solution;

        solution = "ola".getBytes();

        // TODO: solution and query should come in t
        //solution = instance.solveSudoku();
        //}

        //String query = "ola";

        RequestCostCache.getInstance().add(query, solution);

        // TODO: find a way to get the requestUuid and replace it in query
        RequestTracker.getInstance().remove(query);

        // Send response to browser.*/

    }
}
