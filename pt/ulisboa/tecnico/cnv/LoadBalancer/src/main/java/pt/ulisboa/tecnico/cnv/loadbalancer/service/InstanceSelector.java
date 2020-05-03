package pt.ulisboa.tecnico.cnv.loadbalancer.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;

import pt.ulisboa.tecnico.cnv.loadbalancer.domain.RequestCost;

import java.util.logging.Logger;

public class InstanceSelector {

    private static InstanceSelector _instance = null;

    private static Logger _logger = Logger.getLogger(InstanceSelector.class.getName());

    // maps the Cost of a Requested to the Difficulty level:
    // Easy: 30% (20 segundos)
    // Medium: 100% shorter time (1 min 30 segundos)
    // Hard: 100% longer time (5 min 40 segundos)

    private AmazonEC2 amazonEC2;

    private InstanceSelector() {

    }

    public static InstanceSelector getInstance() {
        if (_instance == null) {
            _instance = new InstanceSelector();
        }
        return _instance;
    }

    public static Instance selectInstance(RequestCost cost) {
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