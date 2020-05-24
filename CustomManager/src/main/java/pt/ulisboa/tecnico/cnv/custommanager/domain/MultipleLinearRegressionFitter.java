package pt.ulisboa.tecnico.cnv.custommanager.domain;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.util.ArrayList;
import java.util.List;

public class MultipleLinearRegressionFitter {

    // dependent variable is a list of pairs (s,u,a) where s represents the puzzle size, u the number of unassigned, a the algorithm
    // a = 1 => BFS, a = 2 => DLX , a = 3 => CP
    private List<double[]> dependentVariable = new ArrayList<double[]>();

    // target will be a list of numbers representing the number of field loads or the request time
    private List<Double> target = new ArrayList<>();

    // the actual regressor
    private OLSMultipleLinearRegression mlr = new OLSMultipleLinearRegression();

    // regression parameters
    private double[] b;


    public void addInstance(double[] instance, Double targetValue) {
        this.dependentVariable.add(instance);
        this.target.add(targetValue);
    }

    public void estimateRegressionParameters() {
        double[][] x = new double[dependentVariable.size()][dependentVariable.get(0).length];
        for (int i = 0; i < x.length; i++)
            x[i] = dependentVariable.get(i);

        double[] y = new double[target.size()];
        for (int i = 0; i < x.length; i++)
            y[i]= target.get(i);

        mlr.newSampleData(y, x);
        b = mlr.estimateRegressionParameters();
        //System.out.printf("f(s,u) = %.2f + %.2fs + %.2fu%n", b[0], b[1], b[2]);
    }

    public double makeEstimation(double s, double u) {
        return b[0] + b[1]*s + b[2]*u;
    }

}
