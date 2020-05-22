package pt.ulisboa.tecnico.cnv.custommanager.domain;

import java.util.concurrent.ConcurrentHashMap;

public class RunningInstanceState {

    // Keeps track of all requests currently being processed by this instance
    // Maps each request to the corresponding request cost
    ConcurrentHashMap<String, RequestCost> _processingRequests = new ConcurrentHashMap<>();

    public RunningInstanceState() {

    }

    // TODO: query or uuid?
    public void addNewRequest(String query, RequestCost cost) {
        _processingRequests.put(query, cost);
    }

    public void removeRequest(String query) {
        _processingRequests.remove(query);
    }

    public long calculateRequestCostSum() {
        long totalEstimatedCost = 0;
        for (RequestCost cost : _processingRequests.values()) {
            totalEstimatedCost += cost.getFieldLoads();
        }
        return totalEstimatedCost;
    }
}
