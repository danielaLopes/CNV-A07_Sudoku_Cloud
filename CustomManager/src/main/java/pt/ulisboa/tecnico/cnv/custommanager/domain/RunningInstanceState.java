package pt.ulisboa.tecnico.cnv.custommanager.domain;

import java.util.concurrent.ConcurrentHashMap;

public class RunningInstanceState {

    String _instanceId;
    // Keeps track of all requests currently being processed by this instance
    // Maps each request to the corresponding request cost
    ConcurrentHashMap<String, RequestCost> _processingRequests = new ConcurrentHashMap<>();
    int _estimatedCpu;

    public RunningInstanceState(String instanceId) {
        _instanceId = instanceId;
        _estimatedCpu = 0;
    }

    public String getInstanceId() {
        return _instanceId;
    }

    // TODO: query or uuid?
    public void addNewRequest(String query, RequestCost cost) {

        _processingRequests.put(query, cost);
        _estimatedCpu += cost.getCpu();
    }

    public void removeRequest(String query) {

        _estimatedCpu -= _processingRequests.get(query).getCpu();
        _processingRequests.remove(query);

    }


    public Long calculateRequestCostSum() {
        Long totalEstimatedCost = 0L;
        for (RequestCost cost : _processingRequests.values()) {
            totalEstimatedCost += cost.getFieldLoads();
        }
        return totalEstimatedCost;
    }

    // TODO: see what to do with estimated cpu
    public int getTotalCpuOccupied() {
        /*int totalCpu
        for (RequestCost cost : _processingRequests.values()) {
            totalEstimatedCost += cost.getFieldLoads();
        }
        return totalEstimatedCost;*/
        return 0;
    }

    // TODO
    public int getTotalCpuAvailable() { return 0; }
}
