package pt.ulisboa.tecnico.cnv.custommanager.service;

import com.amazonaws.services.ec2.model.Instance;

import java.util.List;
import java.util.logging.Logger;

public class HealthChecker implements Runnable {

    private static Logger _logger = Logger.getLogger(AutoScaler.class.getName());

    @Override
    public void run() {
        _logger.info("Running health checking...");

        // loops through all instances to healthcheck
        List<Instance> runningInstances = InstanceSelector.getInstance().getRunningInstances();
        for (Instance instance : runningInstances) {
            SendMessages.getInstance().sendHealthCheck(instance, );
        }
    }
}
