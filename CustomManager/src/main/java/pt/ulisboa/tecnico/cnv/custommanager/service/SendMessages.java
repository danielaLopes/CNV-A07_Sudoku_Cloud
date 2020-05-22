package pt.ulisboa.tecnico.cnv.custommanager.service;

import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class SendMessages {

    private static SendMessages _instance = null;

    private static Logger _logger = Logger.getLogger(SendMessages.class.getName());

    private static final int CONNECTION_TIMEOUT = 6000;

    public static SendMessages getInstance() {
        if (_instance == null) {
            _instance = new SendMessages();
        }
        return _instance;
    }

    private SendMessages() {}

    public static void sendClientResponse(final HttpExchange t, byte[] solution) throws IOException {

        final Headers hdrs = t.getResponseHeaders();

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

        _logger.info("Sent response to " + t.getRemoteAddress().toString());
    }

    //public static int sendHealthCheck(Instance instance, String request) throws Exception {
    public static int sendHealthCheck(Instance instance) throws IOException {

        //String urlString = "http://" + instance.getPublicIpAddress() + ":8000/ping?" + request;
        String urlString = "http://" + instance.getPublicIpAddress() + ":8000/ping";

        return getResponseCode(sendServerRequest(false, urlString, null, null));
    }

    public static int sendLocalHealthCheck() throws IOException  {

        String urlString = "http://127.0.0.1:8000/ping";

        return getResponseCode(sendServerRequest(false, urlString, null, null));
    }

    public static int sendSudokuRequest(Instance instance, String request, byte[] body) throws IOException {

        String urlString = "http://" + instance.getPublicIpAddress() + ":8000/sudoku";

        return getResponseCode(sendServerRequest(true, urlString, request, body));
    }

    public static byte[] sendLocalSudokuRequest(String request, byte[] body) throws IOException  {

        String urlString = "http://127.0.0.1:8000/sudoku";
        _logger.info("urlString: " + urlString);

        return getResponse(sendServerRequest(true, urlString, request, body));
    }

    public static HttpURLConnection sendServerRequest(boolean hasBody, String urlString, String urlParameters, byte[] body) throws IOException {

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        if(hasBody == true) {

            byte[] urlParametersBytes = urlParameters.getBytes( StandardCharsets.UTF_8 );

            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            //connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Access-Control-Allow-Origin", "*");

            connection.setRequestProperty("Access-Control-Allow-Credentials", "true");
            connection.setRequestProperty("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
            connection.setRequestProperty("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
            connection.setRequestProperty( "Content-Length", Integer.toString(body.length));
            connection.setDoOutput(true);

            _logger.info("BODY SIZE: " + body.length);

            try(OutputStream os = connection.getOutputStream()) {
                os.write(body, 0, body.length);
            }
        }

        connection.setUseCaches(false);
        //connection.setDoInput(true);

        //connection.setReadTimeout(1000*60*15);
        //connection.setConnectTimeout(CONNECTION_TIMEOUT);

        if (connection != null) {
            connection.disconnect();
        }

        return connection;
    }

    public static byte[] getResponse(HttpURLConnection connection) throws IOException {
        //Get Response
        DataInputStream is = new DataInputStream((connection.getInputStream()));

        byte[] buffer = new byte[connection.getContentLength()];
        is.readFully(buffer);

        return buffer;
    }

    public static int getResponseCode(HttpURLConnection connection) throws IOException {

        return connection.getResponseCode();
    }
}
