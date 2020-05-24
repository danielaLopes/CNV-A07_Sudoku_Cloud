package pt.ulisboa.tecnico.cnv.custommanager.domain;

import java.util.ArrayList;
import java.util.List;

public class PuzzleAlgorithmProperty {

    // field loads for 0 unassigned
    private Long fieldLoads;

    // list containing the outer limits of this puzzle-algorithm intervals
    private List<Integer> intervalsLimits;

    // list containing the adjustment for each interval
    private List<Integer> intervalsAdjustments;

    // list containing the average increase in loads for each extra unassigned
    private List<Integer> intervalsAverageIncrease;

    public PuzzleAlgorithmProperty(Long fieldLoads, List<Integer> intervalsLimits, List<Integer> intervalsAdjustments,
                                   List<Integer> intervalsAverageIncrease) {
        this.fieldLoads = fieldLoads;
        this.intervalsLimits = intervalsLimits;
        this.intervalsAdjustments = intervalsAdjustments;
        this.intervalsAverageIncrease = intervalsAverageIncrease;
    }

    public Long computeEstimatedFieldLoads(Integer requestUnassigned) {

        Long estimatedFieldLoads = fieldLoads;
        try {
            // first we find out to which interval it belongs
            for (int i = 0; i < intervalsLimits.size(); i++) {
                if (intervalsLimits.get(i) > requestUnassigned) {

                    int previousIntervalindex = i - 1;
                    if (previousIntervalindex < 0) previousIntervalindex = 0;

                    Integer intervalGap = requestUnassigned - intervalsLimits.get(previousIntervalindex);
                    // if it belongs to the current interval, we only accumulate part of the interval
                    estimatedFieldLoads += (intervalsAverageIncrease.get(i) * intervalGap + intervalsAdjustments.get(i));
                    break;
                }
                int previousLimit;
                if(i == 0) previousLimit = 0;
                else previousLimit = intervalsLimits.get(i-1);
                // if it belongs to the next interval, we accumulate the full interval
                estimatedFieldLoads += ((intervalsAverageIncrease.get(i) * (intervalsLimits.get(i) - previousLimit)) + intervalsAdjustments.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("prediction:" + estimatedFieldLoads);
        return estimatedFieldLoads;

    }
}
