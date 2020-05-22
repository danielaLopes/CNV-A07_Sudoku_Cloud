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

    private static RequestCostCache _instance = null;

    private static final int MAX_SIZE = 50;
    private static final ConcurrentHashMap<String, RequestCost> _cache = new ConcurrentHashMap<>();

    private RequestCostCache() {}

    public static RequestCostCache getInstance() {
        if (_instance == null) {
            _instance = new RequestCostCache();
        }
        return _instance;
    }

    public static synchronized void add(String key, RequestCost response) {
        if (_cache.size() >= MAX_SIZE) {
            _cache.put(key, response);
        }
        else {
            // select random element to remove
            removeRandom();
        }
    }

    private static void remove(String key) {
        _cache.remove(key);
    }

    public static RequestCost get(String key) {
        return _cache.get(key);
    }

    private static void removeRandom() {
        Random r = new Random();
        int indexToRemove = r.nextInt((MAX_SIZE) + 1);

        List<String> keys = new ArrayList<>(_cache.keySet());
        remove(keys.get(indexToRemove));
    }
}
