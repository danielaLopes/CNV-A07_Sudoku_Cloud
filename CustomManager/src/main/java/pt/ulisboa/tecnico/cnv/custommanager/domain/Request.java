package pt.ulisboa.tecnico.cnv.custommanager.domain;

import com.sun.net.httpserver.HttpExchange;

public class Request {

    private RequestCost cost;

    // necessary in case a request needs to be repeated
    private String query;
    private HttpExchange clientCommunication;
    private byte[] body;

    public Request(RequestCost cost, String query, HttpExchange clientCommunication, byte[] body) {
        this.cost = cost;
        this.query = query;
        this.clientCommunication = clientCommunication;
        this.body = body;
    }

    public RequestCost getCost() { return this.cost; }

    public String getQuery() { return this.query; }

    public HttpExchange getClientCommunication() { return this.clientCommunication; }

    public byte[] getBody() { return body; }
}
