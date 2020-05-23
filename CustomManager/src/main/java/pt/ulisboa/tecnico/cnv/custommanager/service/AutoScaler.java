package pt.ulisboa.tecnico.cnv.custommanager.service;

import pt.ulisboa.tecnico.cnv.custommanager.domain.RunningInstanceState;

import java.util.List;
import java.util.logging.Logger;

public class AutoScaler implements Runnable {
    private static Logger _logger = Logger.getLogger(AutoScaler.class.getName());

    private static final Long AVERAGE_FIELD_LOADS_TO_SCALE_UP = 1000L;
    private static final Long AVERAGE_FIELD_LOADS_TO_SCALE_DOWN = 10000L;

    // Constants to limit instances
    private final int MAX_INSTANCES = 5;
    private final int MIN_INSTANCES = 1;

    /*private static int consecutivePeriodsUpScale = 0;
    private static int consecutivePeriodsDownScale = 0;*/

    @Override
    public void run() {
        _logger.info("Running auto scaler...");

        // TODO: it should either scale up or scale down? Not both ???
        // Makes sure we are running the right number of instances
        int nRunningInstances = InstanceSelector.getInstance().assertRunningInstancesBetweenMinMax();

        // makes sure a scale up or scale down is necessary
        Long averageFieldLoads = InstanceSelector.getInstance().averageFieldLoads();
        if (averageFieldLoads < AVERAGE_FIELD_LOADS_TO_SCALE_DOWN) {
            // we are ale to perform scale down
            if (nRunningInstances - 1 >= MIN_INSTANCES) {
                List<RunningInstanceState> idleInstanceStates =
                        InstanceSelector.getInstance().selectInstancesToTerminate();

                // downscale in case there are idle instances
                if (idleInstanceStates.size() > 0) downScale(idleInstanceStates);
            }
        }
        else if (averageFieldLoads >= AVERAGE_FIELD_LOADS_TO_SCALE_UP) {
            // we are ale to perform scale up
            if (nRunningInstances + 1 <= MAX_INSTANCES) {
                List<RunningInstanceState> overloadedInstanceStates =
                        InstanceSelector.getInstance().selectInstancesToTerminate();

                // upscale in case there are overloaded instances
                if (overloadedInstanceStates.size() > 0) upScale();
            }
        }
        else {
            _logger.info("No need to perform scale up or scale down");
        }

        InstanceSelector.getInstance().resetFieldLoads();

        /*int averageUsage = InstanceSelector.getInstance().getAverageUsage();
        int ratioIdleInstances = InstanceSelector.getInstance().ratioIdleInstances();
        _logger.info("Average usage is " + averageUsage);

        if (averageUsage > UPSCALE_USAGE_THRESHOLD && ratioIdleInstances < 0.5) {
            consecutivePeriodsDownScale = 0;
            consecutivePeriodsUpScale++;
            if (consecutivePeriodsUpScale == 2) {
                consecutivePeriodsUpScale = 0;
                _logger.info("Applying upscale policy...");
                upScale();
            }
        } else if (averageUsage < DOWNSCALE_USAGE_THRESHOLD) {
            consecutivePeriodsUpScale= 0;
            consecutivePeriodsDownScale ++;
            if (consecutivePeriodsDownScale  == 2) {
                consecutivePeriodsDownScale = 0;
                _logger.info("Applying downscale policy...");
                downScale();
            }
        }*/
    }

    public void upScale() {
        _logger.info("Scaling up");
        InstanceSelector.getInstance().startInstances(1);
    }

    // TODO: only download 1 at each time?
    public void downScale(List<RunningInstanceState> idleInstanceStates) {
        _logger.info("Scaling down");
        InstanceSelector.getInstance().removeInstance(idleInstanceStates.get(idleInstanceStates.size()));
    }
}
