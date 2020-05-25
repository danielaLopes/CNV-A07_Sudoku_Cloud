package pt.ulisboa.tecnico.cnv.custommanager.domain;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LinearRegressionFitter {

    // dependent variable is a list of numbers representing the number of Field Loads
    private List<Double> dependentVariable = new ArrayList<Double>();

    // target will be a list of numbers representing the percentage of CPU
    private List<Double> target = new ArrayList<>();

    // the actual regressor
    private SimpleRegression sr = new SimpleRegression();

    // regression parameters
    private double[] b = new double[2];

    private int puzzleSize;

    public LinearRegressionFitter() { }

    public LinearRegressionFitter(int size) {
        this.puzzleSize = size;
    }

    public void addInstance(Double instance, Double targetValue) {
        this.dependentVariable.add(instance);
        this.target.add(targetValue);
    }

    public void estimateRegressionParameters() {
        double[][] x = new double[dependentVariable.size()][1];
        for (int i = 0; i < x.length; i++)
            x[i][0] = dependentVariable.get(i);

        double[] y = new double[target.size()];
        for (int i = 0; i < x.length; i++)
            y[i]= target.get(i);

        //System.out.println(Arrays.toString(x));
        //System.out.println(Arrays.toString(y));
        sr.addObservations(x, y);
        //sr.newSampleData(y, x);
        b[0] = sr.getSlope();
        b[1] = sr.getIntercept();
        //System.out.printf("f(fl) = %.10ffl + %.2f%n", b[0], b[1]);
    }

    public double makeEstimation(double fl) {
        return b[0]*fl + b[1];
    }
}
