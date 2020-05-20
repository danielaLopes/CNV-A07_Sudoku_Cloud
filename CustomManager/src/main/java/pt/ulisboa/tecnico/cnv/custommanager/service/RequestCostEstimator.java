package pt.ulisboa.tecnico.cnv.custommanager.service;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import pt.ulisboa.tecnico.cnv.custommanager.domain.PuzzleAlgorithmProperty;
import pt.ulisboa.tecnico.cnv.custommanager.domain.RequestCost;

public class RequestCostEstimator {

    private static AmazonDynamoDB client;
    private static DynamoDB dynamoDB;
    private static Table fieldLoadsTable;
    private static String tableName = "field-loads-table";

    private static RequestCostEstimator _instance = null;

    private static Logger _logger = Logger.getLogger(InstanceSelector.class.getName());

    // key: algorithm_puzzle, value: PuzzleAlgorithmProperty
    // example: BFS_9X9_101
    private static ConcurrentMap<String, PuzzleAlgorithmProperty> costEstimationsConstants;

    // an entry for each possible combination of algorithm and puzzle
    //public ConcurrentMap<String, CostEstimation> costEstimations = new ConcurrentHashMap<>();

    private RequestCostEstimator() {

        fillCostEstimations();

        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        client = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion("us-east-1")
                .build();

        dynamoDB = new DynamoDB(client);

        verifyTable();

        fieldLoadsTable = dynamoDB.getTable(tableName);


        _logger.info("Client, dynamoDB and table configured successfully.");
    }

    public static RequestCostEstimator getInstance() {
        if (_instance == null) {
            _instance = new RequestCostEstimator();
        }
        return _instance;
    }

    public static RequestCost estimateCost(String query) {
        
        _logger.info("Estimating request cost");
        // Check if the cost of the current request was already stored in the Dynamo
        Item item = getFromDynamo(query);
        //System.out.println(item.toString());

        // if the number of field loads for this request has already been stored
        if(item != null) return computeCPUPercentage(Long.parseLong(item.get("fieldLoads").toString()));


        // check if the request is a known puzzle
        String requestAlgorithmPuzzle = extractAlgorithmPuzzle(query);
        Integer requestUnassigned = extractUnassigned(query);
        _logger.info("Request " + requestAlgorithmPuzzle + " with "+ requestUnassigned + " unasigned not present in database.");
        PuzzleAlgorithmProperty requestProperties = costEstimationsConstants.get(requestAlgorithmPuzzle);
        if(requestProperties != null) {
            Long estimatedFieldLoads = requestProperties.computeEstimatedFieldLoads(requestUnassigned);
            return computeCPUPercentage(estimatedFieldLoads);
        }

        // MISSING: HANDLE UNKNOWN PUZZLES

        return null;
    }

    public static Item getFromDynamo(String query) {
        _logger.info("Retrieving item from database: " + query);
        Item item = null;

        try {
            item = fieldLoadsTable.getItem("requestID", query);
            _logger.info("Retrieved item from database: " + item.toString());
        } catch (Exception e) {
            _logger.info("Unable to retrieve item: " + query + " from database.");
        }

       return item;
    }

    public static RequestCost computeCPUPercentage(Long fieldLoads) {
        _logger.info("Computing cPU percentage from field loads: " + fieldLoads);
        // some equation here to convert the fieldLoads in %CPU
        return new RequestCost(fieldLoads);
    }

    public void fillCostEstimations() {
        // put entries in the map
    }

    public void verifyTable() {
        CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement().withAttributeName("requestID").withKeyType(KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("requestID").withAttributeType(ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

        // Create table if it does not exist yet
        TableUtils.createTableIfNotExists(client, createTableRequest);
        // wait for the table to move into ACTIVE state
        try {
            TableUtils.waitUntilActive(client, tableName);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String extractAlgorithmPuzzle(String query) {
        //String[] queryAttributes = query.split("\\?")[1].split("&");

        String[] queryAttributes = query.split("&");
        String algorithmName = queryAttributes[0].split("=")[1];

        String[] puzzle = queryAttributes[4].split("=")[1].split("_");
        String puzzleName = puzzle[2] + "_" + puzzle[3];

        return algorithmName + "_" + puzzleName;
    }

    public static Integer extractUnassigned(String query) {
        //String[] queryAttributes = query.split("\\?")[1].split("&");
        String[] queryAttributes = query.split("&");

        return Integer.parseInt(queryAttributes[1].split("=")[1]);
    }
}