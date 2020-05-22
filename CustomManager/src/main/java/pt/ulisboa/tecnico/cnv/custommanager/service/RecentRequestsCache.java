package pt.ulisboa.tecnico.cnv.custommanager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps a request query (without the algorithm to the corresponding
 * solution of the most recent requests so that recent equivalent
 * requests are not repeated.
 * Has a random eviction policy.
 */
public class RecentRequestsCache {

    private static RecentRequestsCache _instance = null;

    private static final int MAX_SIZE = 50;
    private static final ConcurrentHashMap<String, byte[]> _cache = new ConcurrentHashMap<>();

    private RecentRequestsCache() {}

    public static RecentRequestsCache getInstance() {
        if (_instance == null) {
            _instance = new RecentRequestsCache();
        }
        return _instance;
    }

    public static synchronized void add(String key, byte[] response) {
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

    public static byte[] get(String key) {
        return _cache.get(key);
    }

    private static void removeRandom() {
        Random r = new Random();
        int indexToRemove = r.nextInt((MAX_SIZE) + 1);

        List<String> keys = new ArrayList<>(_cache.keySet());
        remove(keys.get(indexToRemove));
    }
}
