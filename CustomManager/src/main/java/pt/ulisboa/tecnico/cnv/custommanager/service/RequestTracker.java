package pt.ulisboa.tecnico.cnv.custommanager.service;

import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestState;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the requests being processed and sweeps the
 * map every SWEEP_PERIOD to check if there are requests
 * that need to be repeated by other machines due to machine
 * failure or delays.
 * When servers deliver the solution to the LoadBalancer, the
 * request is removed from the map.
 * Every request is uniquely identified
 */
public class RequestTracker {

    private static RequestTracker _instance = null;

    private static final int SWEEP_PERIOD = 5;
    private final ConcurrentHashMap<String, RequestState> _cache = new ConcurrentHashMap<>();

    private RequestTracker() {}

    public static RequestTracker getInstance() {
        if (_instance == null) {
            _instance = new RequestTracker();
        }
        return _instance;
    }

    public void add(String key, RequestState value) {

    }

    public void remove(String key) {
        _cache.remove(key);
    }

    public RequestState get(String key) {
        return _cache.get(key);
    }
}
