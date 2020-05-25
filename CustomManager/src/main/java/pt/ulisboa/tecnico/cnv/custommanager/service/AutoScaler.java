package pt.ulisboa.tecnico.cnv.custommanager.service;

import pt.ulisboa.tecnico.cnv.custommanager.domain.RunningInstanceState;

import java.util.List;
import java.util.logging.Logger;

public class AutoScaler implements Runnable {
    private static Logger _logger = Logger.getLogger(AutoScaler.class.getName());

    private static final Long AVERAGE_FIELD_LOADS_TO_SCALE_UP = 90000L; // avg 80%CPU
    private static final Long AVERAGE_FIELD_LOADS_TO_SCALE_DOWN = 20000L; // avg 20%CPU

    // Constants to limit instances
    private final int MAX_INSTANCES = 5;
    private final int MIN_INSTANCES = 1;

    private int consecutiveScaleUps = 0;
    private int consecutiveScaleDowns = 0;

    @Override
    public void run() {
        _logger.info("Running auto scaler...");

        // Makes sure we are running the right number of instances
        int nRunningInstances = InstanceSelector.getInstance().assertRunningInstancesBetweenMinMax();

        // makes sure a scale up or scale down is necessary
        Long averageFieldLoads = InstanceSelector.getInstance().averageFieldLoads();
        _logger.info("Average Field Loads: " + averageFieldLoads);
        if (averageFieldLoads <= AVERAGE_FIELD_LOADS_TO_SCALE_DOWN) {
            consecutiveScaleUps = 0;
            // we are ale to perform scale down
            if (nRunningInstances - 1 >= MIN_INSTANCES) {
                List<RunningInstanceState> idleInstanceStates =
                        InstanceSelector.getInstance().selectInstancesToTerminate();

                // downscale in case there are idle instances
                if (idleInstanceStates.size() > 0) {
                    consecutiveScaleDowns++;
                    _logger.info("Incrementing consecutive scaleDowns to " + consecutiveScaleDowns);
                    if (consecutiveScaleDowns >= 4) {
                        consecutiveScaleDowns = 0;
                        downScale(idleInstanceStates);
                    }
                }   
            }
        }
        else if (averageFieldLoads >= AVERAGE_FIELD_LOADS_TO_SCALE_UP) {
            consecutiveScaleDowns = 0;
            // we are ale to perform scale up
            if (nRunningInstances + 1 <= MAX_INSTANCES) {
                List<RunningInstanceState> overloadedInstanceStates =
                        InstanceSelector.getInstance().selectOverloadedInstances();
                
                // upscale in case there are overloaded instances
                if (overloadedInstanceStates.size() > 0) {
                    consecutiveScaleUps++;
                    _logger.info("Incrementing consecutive scaleUps to " + consecutiveScaleUps);
                    if (consecutiveScaleUps >= 2) {
                        consecutiveScaleUps = 0;
                        upScale();
                    }
                }
            }
        }
        else {
            consecutiveScaleDowns = 0;
            consecutiveScaleUps = 0;
            _logger.info("No need to perform scale up or scale down");
        }

        InstanceSelector.getInstance().resetFieldLoads();
    }

    public void upScale() {
        _logger.info("Scaling up");
        InstanceSelector.getInstance().startInstances(1);
    }

    public void downScale(List<RunningInstanceState> idleInstanceStates) {
        _logger.info("Scaling down");
        // removes instance with least latestFieldLoads so that the most idle machine is terminated
        InstanceSelector.getInstance().removeInstance(idleInstanceStates.get(0));
    }
}
