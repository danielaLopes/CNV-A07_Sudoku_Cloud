package pt.ulisboa.tecnico.cnv.custommanager.handler;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.logging.Logger;

public class ResponseHandler implements HttpHandler {

    private static Logger logger = Logger.getLogger(ResponseHandler.class.getName());

    @Override
    public void handle(final HttpExchange t) throws IOException {

        logger.info("Handling response to request");

        byte[] solution;

        solution = "ola".getBytes();

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
