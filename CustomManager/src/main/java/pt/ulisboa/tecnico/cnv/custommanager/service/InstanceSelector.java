package pt.ulisboa.tecnico.cnv.custommanager.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RunningInstanceState;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class InstanceSelector {

    private static InstanceSelector _instance = null;

    private Logger _logger = Logger.getLogger(InstanceSelector.class.getName());

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

    private final String WEB_SERVER_AMI = "ami-0d078ba2b9844aef0";

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

    public List<RunningInstanceState> getRunningInstanceStates() {
        return new ArrayList<>(_runningInstances.values());
    }

    public List<Instance> getRunningInstances() {
        List<Instance> instances = new ArrayList<>();
        for (String instanceId : _runningInstances.keySet()) {
            instances.add(_instances.get(instanceId));
        }
        return instances;
    }

    public Instance getInstanceById(String instanceId) {
        return _instances.get(instanceId);
    }

    /**
     * Checks if there are instances getting initialized for when instances are overloaded, requests go to the new ones
     * @return
     */
    public boolean instancesInitializing() {

        List<RunningInstanceState> instanceStates = new ArrayList<>(_runningInstances.values());
        for (RunningInstanceState instanceState : instanceStates) {
            if (instanceState.isInitialized() == false) {
                _logger.info("Instance is still initializing...");
                return true;
            }
        }
        return false;
    }

    public static boolean checkInstanceInitialized(RunningInstanceState instanceState) {

        getInstance()._logger.info("Checking if instance is initialized");

        Instance instance = getInstance().getInstanceById(instanceState.getInstanceId());

        int code = 0;
        try {
            code = SendMessages.getInstance().sendHealthCheck(instance);
        }
        catch(IOException e) {
            getInstance()._logger.info(e + "");
            return false;
        }

        if (code == 200) {
            instanceState.setInitialized();
            getInstance()._logger.info("Instance is initialized!");
            return true;
        }
        else return false;
    }

    // -------------------------------------------------------------
    // -----            Methods to manage instances            -----
    // -------------------------------------------------------------

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
                // Checks if it's not the LoadBalancer
                if (instance.getImageId().equals(WEB_SERVER_AMI)) {
                    _logger.info("Adding instance " + instance.getInstanceId() +
                        " with state " + instance.getState());
                    _instances.put(instance.getInstanceId(), instance);
                    if (instance.getState().getName().equals("running")) {
                        _runningInstances.put(instance.getInstanceId(), new RunningInstanceState(instance.getInstanceId()));
                    }
                }
            }
        }
    }

    public synchronized int assertRunningInstancesBetweenMinMax() {
        int nRunningInstances = _runningInstances.size();
        if (nRunningInstances > MAX_INSTANCES) {
            int numToTerminate = nRunningInstances - MAX_INSTANCES;
            _logger.info("Too many instances running (" + nRunningInstances + ") terminating " + numToTerminate);
            removeInstances(numToTerminate);
        } else if (nRunningInstances < MIN_INSTANCES) {
            int numToLaunch = MIN_INSTANCES - nRunningInstances;
            _logger.info("Too few instances running (" + nRunningInstances + ") launching " + numToLaunch);
            startInstances(numToLaunch);
        }
        return _runningInstances.size();
    }

    public void startInstances(int n) {

        _logger.info("Currently running " + _runningInstances.size() + " instances.");

        if (_runningInstances.size() + n > MAX_INSTANCES) {
            _logger.warning("Not possible to start new instance: already running MAX instances");
            return;
        }

        _logger.info("Starting " + n + " instances");

        RunInstancesRequest runInstancesRequest = new RunInstancesRequest();

        runInstancesRequest.withImageId(WEB_SERVER_AMI)
                            .withMinCount(n)
                            .withMaxCount(n)
                            .withKeyName("CNV-proj")
                            .withSecurityGroupIds("sg-0bf52fd8f7cb92397")
                            .withInstanceType("t2.micro");

        RunInstancesResult runInstancesResult = _ec2.runInstances(runInstancesRequest);

        for (Instance instance: runInstancesResult.getReservation().getInstances()) {
            _logger.info("Started instance " + instance.getInstanceId());
            _instances.put(instance.getInstanceId(), instance);
            _runningInstances.put(instance.getInstanceId(), new RunningInstanceState(instance.getInstanceId()));
        }
    }

    public synchronized void removeInstances(int numInstances) {
        int nRunningInstances = _runningInstances.size();
        if (nRunningInstances - numInstances < MIN_INSTANCES) {
            return;
        } else {
            _logger.info("Going to remove " + numInstances + " out of " + nRunningInstances);
        }

        List<RunningInstanceState> instanceStates = new ArrayList<>(_runningInstances.values());

        Collections.sort(instanceStates, RunningInstanceState.LEAST_CPU_AVAILABLE_COMPARATOR);

        List<RunningInstanceState> instancesToRemove = instanceStates.subList(
                instanceStates.size() - numInstances, instanceStates.size());
        // for debug
        for (RunningInstanceState s : instanceStates) _logger.info("instanceStates " + s.getTotalCpuOccupied());
        for (RunningInstanceState s : instancesToRemove) _logger.info("instancesToRemove " + s.getTotalCpuOccupied());

        for (RunningInstanceState instanceState : instancesToRemove) {

            removeInstance(instanceState);
        }
    }

    public void removeInstance(RunningInstanceState instanceState) {

        _logger.info("Scheduling shutdown for instance " + instanceState.getInstanceId() +
                " with cpu of " + instanceState.getTotalCpuOccupied());
        instanceState.scheduleShutdown();
    }

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

    public void shutdown() {
        _logger.info("Shutting down all instances...");

        DescribeInstancesResult describeInstancesRequest = _ec2.describeInstances();
        List<Reservation> reservations = describeInstancesRequest.getReservations();

        for (Reservation reservation : reservations) {
            List<Instance> instancesToTerminate = reservation.getInstances();
            for (Instance instance : instancesToTerminate) {
                _logger.info("Terminating instance " + instance.getInstanceId() +
                        " with state " + instance.getState());
                // TODO: do we need to cleanup memory due to being static or somethig??
                terminateInstance(instance.getInstanceId());
            }
        }
    }

    public List<RunningInstanceState> selectActiveInstanceStates() {

        List<RunningInstanceState> instanceStates = new ArrayList<>();
        for (RunningInstanceState instanceState : _runningInstances.values()) {
            // only selects machines that weren't scheduled to shutdown and fully initialized
            if (instanceState.shuttingDown() == false && instanceState.isInitialized()) {
                instanceStates.add(instanceState);
            }
        }

        return instanceStates;
    }

    public void replaceFailedInstance(String instanceId) {
        terminateInstance(instanceId);
        startInstances(1);
    }

    public Instance describeInstances(String instanceId) {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        DescribeInstancesResult result = _ec2.describeInstances(request);

        List<Reservation> reservations = result.getReservations();

        for (Reservation reservation: reservations) {
            List<Instance> instancesList = reservation.getInstances();

            for (Instance instance: instancesList) {
                // to update instances public IPs after they are initialized
                if (instance.getInstanceId().equals(instanceId)) {
                    _logger.info("Replaced instance info!");
                    _instances.put(instance.getInstanceId(), instance);
                    return instance;
                }
            }
        }
        return null;
    }

    // -------------------------------------------------------------
    // -----  Methods to help the LoadBalancer choose which    -----
    // -----            instance to run a request              -----
    // -------------------------------------------------------------

    public synchronized RunningInstanceState selectInstance(RequestCost cost) {

        List<RunningInstanceState> instanceStates = selectActiveInstanceStates();

        // If there are requests before machines are up and running, it waits
        if (instanceStates.isEmpty()) { return null; }

        Collections.sort(instanceStates, RunningInstanceState.LEAST_CPU_AVAILABLE_COMPARATOR);

        for (RunningInstanceState instanceState : instanceStates) {
            _logger.info(instanceState.getInstanceId() + " ORDERED BY SELECTINSTANCE() " + instanceState.getTotalCpuAvailable());
        }

        for (RunningInstanceState instanceState : instanceStates) {
            // if machine has enough CPU available
            // chooses the machine with least cpu available that has enough available cpu to process the request
            if (instanceState.getTotalCpuAvailable() >= cost.getCpuPercentage()) {
                _logger.info(instanceState.getInstanceId() + " has enough CPU available and was choosen with " + instanceState.getTotalCpuAvailable());
                return instanceState;
            }
        }
        
        Collections.sort(instanceStates, RunningInstanceState.LEAST_SUM_PROCESSING_FIELD_LOADS_COMPARATOR);

        // if no machine has enough CPU available and no new machines are being initialized,
        // chooses the one with most CPU available, even if that's above 100
        return instanceStates.get(0);
    }

    // -------------------------------------------------------------
    // ----- Methods to help the AutoScaler to choose machines -----
    // -------------------------------------------------------------
    public List<RunningInstanceState> selectInstancesToTerminate() {

        List<RunningInstanceState> instanceStates = selectActiveInstanceStates();
        // If there are no instances, does not choose any instance to terminate
        if (instanceStates.isEmpty()) { return null; }

        // select idle instances
        List<RunningInstanceState> idleInstanceStates = new ArrayList<>();
        for (RunningInstanceState instanceState : instanceStates) {
            if (instanceState.isIdle()) {
                idleInstanceStates.add(instanceState);
            }
        }
        Collections.sort(instanceStates, RunningInstanceState.LEAST_LATEST_FIELD_LOADS_COMPARATOR);

        for (RunningInstanceState instanceState : instanceStates) {
            _logger.info(instanceState.getInstanceId() + " ORDERED BY SELECTINSTANCESTOTERMINATE() " + instanceState.getTotalFieldLoadsByPeriod());
        }

        return idleInstanceStates;
    }

    public List<RunningInstanceState> selectOverloadedInstances() {

        List<RunningInstanceState> instanceStates = selectActiveInstanceStates();
        // If there are no instances, does not choose any instance to terminate
        if (instanceStates.isEmpty()) { return null; }

        // select overloaded instances
        List<RunningInstanceState> overloadedInstanceStates = new ArrayList<>();
        for (RunningInstanceState instanceState : instanceStates) {
            if (instanceState.isOverloaded()) {
                overloadedInstanceStates.add(instanceState);
            }
        }
        Collections.sort(instanceStates, RunningInstanceState.LEAST_LATEST_FIELD_LOADS_COMPARATOR);

        for (RunningInstanceState instanceState : instanceStates) {
            _logger.info(instanceState.getInstanceId() + " ORDERED BY SELECTOVERLOADEDINSTANCES " + instanceState.getTotalFieldLoadsByPeriod());
        }

        return overloadedInstanceStates;
    }

    public Long averageFieldLoads() {
        Long sumFieldLoads = 0L;
        for (RunningInstanceState instanceState : _runningInstances.values()) {
            // amount of actual field loads from requests that were processed since the previous AutoScaler run
            sumFieldLoads += instanceState.getLatestFieldLoads();
            // amount of estimated field loads from requests currently being processed
            sumFieldLoads += instanceState.calculateRequestCostSum();
        }
        return sumFieldLoads / new Long( _runningInstances.size());
    }

    public void resetFieldLoads() {
        for (RunningInstanceState instanceState : _runningInstances.values()) {
            instanceState.resetFieldLoads();
        }
    }
}