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

public class SendMessages {

    private static SendMessages _instance = null;

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

    //public static int sendHealthCheck(Instance instance, String request) throws Exception {
    public static int sendHealthCheck(Instance instance) throws Exception {

        HttpURLConnection connection = null;

        //String urlString = "http://" + instance.getPublicIpAddress() + ":8000/ping?" + request;
        String urlString = "http://" + instance.getPublicIpAddress() + ":8000/ping";

        URL url = new URL(urlString);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        /*connection.setUseCaches(false);
        connection.setDoInput(true);

        connection.setReadTimeout(1000*60*15);*/
        connection.setConnectTimeout(CONNECTION_TIMEOUT);

        //Get Response
        DataInputStream is = new DataInputStream((connection.getInputStream()));

        byte[] buffer = new byte[connection.getContentLength()];
        is.readFully(buffer);


        if (connection != null) {
            connection.disconnect();
        }

        int code = connection.getResponseCode();

        return code;
    }
}
