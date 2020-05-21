package pt.ulisboa.tecnico.cnv.custommanager.service;

import java.util.logging.Logger;

public class HealthChecker implements Runnable {

    private static Logger _logger = Logger.getLogger(AutoScaler.class.getName());

    @Override
    public void run() {
        _logger.info("Running health checking...");

        // loops through all instances to healthcheck
    }

    public void ping() {

    }
}
