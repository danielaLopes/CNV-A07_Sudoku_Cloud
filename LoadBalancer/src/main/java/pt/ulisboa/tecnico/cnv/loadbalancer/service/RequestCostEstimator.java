package pt.ulisboa.tecnico.cnv.loadbalancer.service;

import java.util.logging.Logger;

import pt.ulisboa.tecnico.cnv.loadbalancer.domain.RequestCost;

public class RequestCostEstimator {

    private static RequestCostEstimator _instance = null;

    private static Logger _logger = Logger.getLogger(InstanceSelector.class.getName());

    // an entry for each possible combination of algorithm and puzzle
    //public ConcurrentMap<String, CostEstimation> costEstimations = new ConcurrentHashMap<>();

    private RequestCostEstimator() {

    }

    public static RequestCostEstimator getInstance() {
        if (_instance == null) {
            _instance = new RequestCostEstimator();
        }
        return _instance;
    }

    public static RequestCost estimateCost(String query) {
        
        _logger.info("Estimating request cost");
        return null;
    }
}