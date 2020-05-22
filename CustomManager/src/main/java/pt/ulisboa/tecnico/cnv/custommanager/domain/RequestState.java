package pt.ulisboa.tecnico.cnv.custommanager.domain;

import com.sun.net.httpserver.HttpExchange;

public class RequestState {

    private HttpExchange _clientCommunication; // so that we can respond to the correct client

    private String _query;
    private String _instanceId;

    private int _expectedTime; // in milliseconds
    private int _nSweeps;

    public RequestState(HttpExchange clientCommunication, String query, String instanceId, int expectedTime) {
        _clientCommunication = clientCommunication;
        _query = query;
        _instanceId = instanceId;
        _expectedTime = expectedTime;
        _nSweeps = 0;
    }

    public HttpExchange getClientCommunication() {
        return _clientCommunication;
    }

    public String getQuery() {
        return _query;
    }

    public String getInstanceId() {
        return _instanceId;
    }

    public int getAndIncrementNSweeps() {
        return _nSweeps++;
    }

    // TODO: add a margin in case request takes longer
    public boolean timeExceeded(int sweepPeriod) {
        return ((_nSweeps * sweepPeriod) > _expectedTime);
    }
}
