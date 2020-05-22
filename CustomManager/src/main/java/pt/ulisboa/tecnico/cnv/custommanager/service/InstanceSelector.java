package pt.ulisboa.tecnico.cnv.custommanager.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RunningInstanceState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class InstanceSelector {

    private static InstanceSelector _instance = null;

    private Logger _logger = Logger.getLogger(InstanceSelector.class.getName());

    // maps the Cost of a Requested to the Difficulty level:
    // Easy: 30% (20 segundos)
    // Medium: 100% shorter time (1 min 30 segundos)
    // Hard: 100% longer time (5 min 40 segundos)

    // AWS data structures
    private AmazonEC2 _ec2;
    private AmazonCloudWatch _cloudWatch;

    // Structures to keep track of instances
    //private AtomicInteger _nRunningInstances = new AtomicInteger(0);
    private ConcurrentMap<String, Instance> _instances = new ConcurrentHashMap<>();
    /** Keeps track of all instances with state running */
    private ConcurrentMap<String, RunningInstanceState> _runningInstances = new ConcurrentHashMap<>();

    // Constants to limit instances
    private final int MAX_INSTANCES = 5;
    private final int MIN_INSTANCES = 1;

    private InstanceSelector() {
        init();
    }

    public static InstanceSelector getInstance() {
        if (_instance == null) {
            _instance = new InstanceSelector();
        }
        return _instance;
    }

    private void init() {
        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();

        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        _ec2 = AmazonEC2ClientBuilder.standard()
                .withRegion("us-east-1")
                //.withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withCredentials(credentialsProvider)
                .build();

        _cloudWatch = AmazonCloudWatchClientBuilder.standard()
                .withRegion("us-east-1")
                //.withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withCredentials(credentialsProvider)
                .build();
    }

    public RunningInstanceState getRunningInstanceState(String instanceId) {
        return _runningInstances.get(instanceId);
    }

    /**
     * Gathers all instances in the AWS Account, no matter the state they are in.
     */
    public void gatherAllInstances() {

        _logger.info("Gathering all instances");

        DescribeInstancesResult describeInstancesRequest = _ec2.describeInstances();
        List<Reservation> reservations = describeInstancesRequest.getReservations();

        for (Reservation reservation : reservations) {
            List<Instance> instancesToAdd = reservation.getInstances();
            for (Instance instance : instancesToAdd) {
                _logger.info("Adding instance " + instance.getInstanceId() +
                        " with state " + instance.getState());
                _instances.put(instance.getInstanceId(), instance);
                if (instance.getState().getName().equals("running")) {
                    _runningInstances.put(instance.getImageId(), new RunningInstanceState());
                }
            }
        }
    }

    public void startInstances(int n) {

        _logger.info("Currently running " + _runningInstances.size() + " instances.");

        if (_runningInstances.size() + n > MAX_INSTANCES) {
            _logger.warning("Not possible to start new instance: already running MAX instances");
            return;
        }

        _logger.info("Starting " + n + " instances");

        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId("ami-07605abcbabe8e9a7")
                            .withMinCount(n)
                            .withMaxCount(n)
                            .withKeyName("CNV")
                            .withSecurityGroupIds("sg-0bf52fd8f7cb92397")
                            .withInstanceType("t2.micro");

        RunInstancesResult runInstancesResult = _ec2.runInstances(runInstancesRequest);

        for (Instance instance: runInstancesResult.getReservation().getInstances()) {
            _logger.info("Started instance " + instance.getInstanceId());
            _instances.put(instance.getInstanceId(), instance);
            _runningInstances.put(instance.getInstanceId(), new RunningInstanceState());
        }
    }

    /*public void shutdownInstances(int n) {

        _logger.info("Shutting down all instances...");
        DescribeInstancesRequest request = new DescribeInstancesRequest();

        List<String> values = new ArrayList<>();
        values.add(AwsConfiguration.getInstance().getWebServerTagValue());
        Filter filter = new Filter("tag:" + AwsConfiguration.getInstance().getWebServerTagValue(), values);
        DescribeInstancesResult result = amazonEC2.describeInstances(request.withFilters(filter));

        List<Reservation> reservations = result.getReservations();

        for (Reservation reservation: reservations) {
            List<Instance> instancesList = reservation.getInstances();

            for (Instance instance: instancesList) {
                if (instance.getState().getName().equals("running")) {
                    logger.info("Shutting instance " + instance.getInstanceId());
                    destroyInstance(instance.getInstanceId());
                }
            }
        }

    }*/

    public void terminateInstance(String instanceId) {

        _logger.info("Terminating instance " + instanceId);
        TerminateInstancesRequest termInstanceReq = new TerminateInstancesRequest();
        termInstanceReq.withInstanceIds(instanceId);
        _ec2.terminateInstances(termInstanceReq);
        _runningInstances.remove(instanceId);
    }

    public void terminateInstances(int n) {

        if (_runningInstances.size() - n < MIN_INSTANCES) {
            _logger.warning("Not possible to terminate instances: number of instances would become lower than MIN");
            return;
        }

        _logger.info("Terminating " + n + " instances");

        synchronized (_runningInstances) {
            /*List<Instance> instances = new ArrayList<>(_instances.values());
            List<String> instancesIds = new ArrayList<>(_instances.keySet());*/
            // TODO: define comparator for instances
            //Collections.sort(instancesList, InstanceState.COMPARATOR_BY_USAGE);
            int nRemoved = 0;
            List<String> instancesIds = new ArrayList<>(_runningInstances.keySet());
            for (String instanceId : instancesIds) {
                terminateInstance(instanceId);
                nRemoved++;

                if (nRemoved == n) {
                    _logger.info("Terminated " + nRemoved + " instances.");
                    return;
                }
            }
        }
    }

    public void terminateAllInstances() {

        _logger.info("Terminating all instances");

        synchronized (_instances) {

            List<String> instancesIds = new ArrayList<>(_runningInstances.keySet());
            for (String instanceId : instancesIds) {
                terminateInstance(instanceId);
            }
        }
    }

    public Instance selectInstance(RequestCost cost) {
        // Easy
        /*if (< cost < ) {
            chooseMostOccupiedMachineWithEnoughCPU();
        }
        // Medium or Hard
        else {
            chooseMachineWithLeastLoad(); // machine is going to be 100% in both difficulties
        }*/
        
        _logger.info("Selecting instance to process request");

        return null;
    }

}