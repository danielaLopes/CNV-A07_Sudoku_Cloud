package pt.ulisboa.tecnico.cnv.custommanager.service;

import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps a request query to the respective instrumented values from the server.
 * Has a random eviction policy.
 */
public class RequestCostCache {

    private final int MAX_SIZE = 20;
    private ConcurrentHashMap<String, RequestCost> _cache = new ConcurrentHashMap<>();

    public RequestCostCache() {}

    public synchronized void put(String key, RequestCost response) {
        if (_cache.size() <= MAX_SIZE) {
            _cache.put(key, response);
        }
        else {
            // select random element to remove
            removeRandom();
        }
    }

    private void remove(String key) {
        _cache.remove(key);
    }

    public RequestCost get(String key) {
        return _cache.get(key);
    }

    private void removeRandom() {
        Random r = new Random();
        int indexToRemove = r.nextInt(MAX_SIZE);

        List<String> keys = new ArrayList<>(_cache.keySet());
        remove(keys.get(indexToRemove));
    }
}
