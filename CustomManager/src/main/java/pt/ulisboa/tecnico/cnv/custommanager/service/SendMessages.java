package pt.ulisboa.tecnico.cnv.custommanager.service;

import com.amazonaws.services.ec2.model.Instance;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class SendMessages {

    private static SendMessages _instance = null;

    private static Logger _logger = Logger.getLogger(SendMessages.class.getName());

    private static final int PING_TIMEOUT = 3000; // milliseconds
    private static final int CONNECTION_TIMEOUT = 10000; // milliseconds

    public static SendMessages getInstance() {
        if (_instance == null) {
            _instance = new SendMessages();
        }
        return _instance;
    }

    private SendMessages() {}

    public static void sendClientResponse(final HttpExchange t, String solution) throws IOException {

        final Headers hdrs = t.getResponseHeaders();

        hdrs.add("Content-Type", "application/json");

        hdrs.add("Access-Control-Allow-Origin", "*");

        hdrs.add("Access-Control-Allow-Credentials", "true");
        hdrs.add("Access-Control-Allow-Methods", "POST, GET, HEAD, OPTIONS");
        hdrs.add("Access-Control-Allow-Headers", "Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

        t.sendResponseHeaders(200, solution.length());

        final OutputStream os = t.getResponseBody();
        OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
        osw.write(solution);
        osw.flush();
        osw.close();

        os.close();

        _logger.info("Sent response to " + t.getRemoteAddress().toString());
    }

    //public static int sendHealthCheck(Instance instance, String request) throws Exception {
    public static int sendHealthCheck(Instance instance) throws IOException {

        String urlString = "http://" + instance.getPublicIpAddress() + ":8000/ping";

        return getResponseCode(sendServerRequest("GET", urlString, null, PING_TIMEOUT));
    }

    public static int sendLocalHealthCheck() throws IOException  {

        String urlString = "http://127.0.0.1:8000/ping";

        return getResponseCode(sendServerRequest("GET", urlString, null, PING_TIMEOUT));
    }

    public static String sendSudokuRequest(
            Instance instance, String request, byte[] body, int estimatedTime) throws IOException {

        String urlString = "http://" + instance.getPublicIpAddress() + ":8000/sudoku?" + request;

        return getResponse(sendServerRequest("POST", urlString, body, estimatedTime));
    }

    public static String sendLocalSudokuRequest(String request, byte[] body, int estimatedTime) throws IOException  {

        String urlString = "http://127.0.0.1:8000/sudoku?" + request;
        _logger.info("urlString: " + urlString);

        return getResponse(sendServerRequest("POST", urlString, body, estimatedTime));
    }

    public static HttpURLConnection sendServerRequest(
            String method, String urlString, byte[] body, int timeout) throws IOException {

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);

        // time to wait for a request to be processed
        connection.setReadTimeout(timeout);

        // time it takes to establish a connection with a server
        connection.setConnectTimeout(CONNECTION_TIMEOUT);

        if(method.equals("POST")) {

            connection.setDoOutput(true);

            try(OutputStream os = connection.getOutputStream()) {
                os.write(body, 0, body.length);
            }
            // TODO: check if it's supposed to be done here
            catch(SocketTimeoutException e) {
                _logger.warning("Timout exceeded to send request");
                throw e;
            }
            catch(IOException e) {
                _logger.warning("Problem sending the request");
                throw e;
            }
        }

        if (connection != null) {
            connection.disconnect();
        }

        return connection;
    }

    public static String getResponse(HttpURLConnection connection) throws IOException {

        if (getResponseCode(connection) == 200) {

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream()));

            StringBuilder response = new StringBuilder();
            String currentLine;

            while ((currentLine = in.readLine()) != null)
                response.append(currentLine);

            in.close();

            return response.toString();
        }
        else {
            return null;
        }
    }

    public static int getResponseCode(HttpURLConnection connection) throws IOException {

        return connection.getResponseCode();
    }
}
