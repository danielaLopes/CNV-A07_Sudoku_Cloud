package pt.ulisboa.tecnico.cnv.custommanager.service;

import java.util.logging.Logger;

public class AutoScaler implements Runnable {
    private static Logger _logger = Logger.getLogger(AutoScaler.class.getName());

    /*private static final int UPSCALE_USAGE_THRESHOLD =
    private static final int DOWNSCALE_USAGE_THRESHOLD =
    private static int consecutivePeriodsUpScale = 0;
    private static int consecutivePeriodsDownScale = 0;*/

    @Override
    public void run() {
        _logger.info("Running auto scaler...");

        // Makes sure we are running the right number of instances
        InstanceSelector.getInstance().assertRunningInstancesBetweenMinMax();

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

    public void downScale() {
        _logger.info("Scaling down");
        InstanceSelector.getInstance().removeInstances(1);
    }
}
