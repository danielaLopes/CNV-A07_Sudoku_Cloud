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
        //List<Instance> runningInstances = InstanceSelector.getInstance().getRunningInstances();
        //for (Instance instance : runningInstances) {
            try {
                //int code = SendMessages.getInstance().sendHealthCheck(instance);
                int code = SendMessages.getInstance().sendLocalHealthCheck();

                _logger.info("HEALTH CODE: " + code);
                if (code == 200) {
                    _logger.info("Instance is healthy!");
                }
                else {
                    _logger.warning("Instance is not healthy!");
                }
            }
            catch(Exception e) {
                //_logger.warning("Failed to send healthcheck to " + instance.getInstanceId());
                _logger.warning(e + " Failed to send healthcheck");
            }
            
        //}
    }
}
