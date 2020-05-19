package pt.ulisboa.tecnico.cnv.custommanager.domain;

import java.util.concurrent.ConcurrentHashMap;

public class RunningInstanceState {

    // Keeps track of all requests currently being processed by this instance
    ConcurrentHashMap<String, String> _processingRequests = new ConcurrentHashMap<>();

    public RunningInstanceState() {

    }

    // TODO: query or uuid?
    public void addNewRequest(String query) {
        _processingRequests.put(query, query);
    }

    public void removeRequest(String query) {
        _processingRequests.remove(query);
    }
}
