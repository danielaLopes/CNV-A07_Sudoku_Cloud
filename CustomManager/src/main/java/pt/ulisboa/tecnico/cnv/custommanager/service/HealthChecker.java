package pt.ulisboa.tecnico.cnv.custommanager.service;

import com.amazonaws.services.ec2.model.Instance;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RunningInstanceState;

import java.util.List;
import java.util.logging.Logger;

public class HealthChecker implements Runnable {

    private static Logger _logger = Logger.getLogger(AutoScaler.class.getName());

    @Override
    public void run() {
        _logger.info("Running health checking...");

        // loops through all instances to healthcheck
        List<RunningInstanceState> runningInstances = InstanceSelector.getInstance().getRunningInstanceStates();
        for (RunningInstanceState instanceState : runningInstances) {
            Instance instance = InstanceSelector.getInstance().getInstanceById(instanceState.getInstanceId())

            try {
                int code = SendMessages.getInstance().sendHealthCheck(instance);
                //int code = SendMessages.getInstance().sendLocalHealthCheck();

                if (code == 200) {
                    _logger.info("Instance " + instanceState.getInstanceId() + " is healthy!");
                    instanceState.resetHealthCheckStrikes();
                }
                else {
                    _logger.warning("Instance " + instanceState.getInstanceId() + " is not healthy!");
                    updateInstanceStateFailed(instanceState);
                }
            }
            catch(Exception e) {
                _logger.warning(e + " Failed to send healthcheck to " + instanceState.getInstanceId());
                updateInstanceStateFailed(instanceState);
            }
        }
    }

    public void updateInstanceStateFailed(RunningInstanceState instanceState) {
        instanceState.incrementHealthCheckStrikes();
        if (instanceState.failed()) {
            InstanceSelector.getInstance().replaceFailedInstance(instanceState.getInstanceId());
        }
    }
}
