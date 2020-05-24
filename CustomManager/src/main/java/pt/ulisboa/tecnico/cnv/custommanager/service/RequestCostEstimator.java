package pt.ulisboa.tecnico.cnv.custommanager.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
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
import pt.ulisboa.tecnico.cnv.custommanager.domain.MultipleLinearRegressionFitter;
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

    // Multiple Linear Regression for new requests
    private static MultipleLinearRegressionFitter fieldLoadFitter;
    private static MultipleLinearRegressionFitter executionTimeFitter;

    // an entry for each possible combination of algorithm and puzzle
    //public ConcurrentMap<String, CostEstimation> costEstimations = new ConcurrentHashMap<>();

    private RequestCostEstimator() {

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

        costEstimationsConstants = new ConcurrentHashMap<>();
        fieldLoadFitter = new MultipleLinearRegressionFitter();
        executionTimeFitter = new MultipleLinearRegressionFitter();

        fillCostEstimations();

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

        String requestAlgorithmPuzzle = extractAlgorithmPuzzle(query);
        Integer requestUnassigned = extractUnassigned(query);
        _logger.info("Request " + requestAlgorithmPuzzle + " with "+ requestUnassigned + " unassigned not present in database.");

        // check if the request is a known puzzle
        PuzzleAlgorithmProperty requestProperties = costEstimationsConstants.get(requestAlgorithmPuzzle);
        if(requestProperties != null) {
            _logger.info("Request " + requestAlgorithmPuzzle + " is a known puzzle");
            Long estimatedFieldLoads = requestProperties.computeEstimatedFieldLoads(requestUnassigned);
            return computeCPUPercentage(estimatedFieldLoads);
        }

        _logger.info("Request " + requestAlgorithmPuzzle + " is an unknown puzzle");
        // if its an unknown puzzle, we make a prediction of the request load
        fieldLoadFitter.estimateRegressionParameters();
        return new RequestCost((long) fieldLoadFitter.makeEstimation(10, 59));
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
        try {
            // put entries in the map and in the fitter
            /*CP_9X9_101*/
            List<Integer> intervalLimits9x9 = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 81);
            List<Integer> adjustments9x9_1 = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0);
            //List<Integer> adjustments9x9_1 = Arrays.asList(-1, -1, -3, -4, -5, -6, -5, -4);
            List<Integer> averageIncreaseCP9x9_1 = Arrays.asList(43, 43, 31, 24, 38, 23, 27, 33);
            PuzzleAlgorithmProperty property = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseCP9x9_1);
            costEstimationsConstants.put("CP_9x9_101", property);
            fieldLoadFitter.addInstance(new double[]{9, 0, 3}, (double) 81);

            /*CP_9X9_102*/
            List<Integer> averageIncreaseCP9x9_2 = Arrays.asList(44, 52, 45, 22, 36, 26, 19, 37);
            PuzzleAlgorithmProperty propertyCP9x9_2 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseCP9x9_2);
            costEstimationsConstants.put("CP_9x9_102", propertyCP9x9_2);

            /*CP_9X9_103*/
            List<Integer> averageIncreaseCP9x9_3 = Arrays.asList(41, 56, 43, 1, 38, 23, 27, 33);
            PuzzleAlgorithmProperty propertyCP9x9_3 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseCP9x9_3);
            costEstimationsConstants.put("CP_9x9_103", propertyCP9x9_3);

            /*CP_9X9_104*/
            List<Integer> averageIncreaseCP9x9_4 = Arrays.asList(41, 46, 19, 23, 32, 24, 52, 30);
            PuzzleAlgorithmProperty propertyCP9x9_4 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseCP9x9_4);
            costEstimationsConstants.put("CP_9x9_104", propertyCP9x9_4);

            /*CP_9X9_105*/
            List<Integer> averageIncreaseCP9x9_5 = Arrays.asList(40, 43, 20, 28, 35, 23, 50, 44);
            PuzzleAlgorithmProperty propertyCP9x9_5 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseCP9x9_5);
            costEstimationsConstants.put("CP_9x9_105", propertyCP9x9_5);

            /*CP_16X16_01*/
            List<Integer> intervalLimits16x16 = Arrays.asList(30, 60, 90, 120, 150, 180, 210, 240, 256);
            List<Integer> adjustments16x16 = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0);
            List<Integer> averageIncreaseCP16x16_1 = Arrays.asList(97, 87, 76, 57, 51, 75, 161, 70, 20);
            PuzzleAlgorithmProperty propertyCP16x16_1 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseCP16x16_1);
            costEstimationsConstants.put("CP_16x16_01", propertyCP16x16_1);
            fieldLoadFitter.addInstance(new double[]{16, 0, 3}, (double) 256);

            /*CP_16X16_02*/
            List<Integer> averageIncreaseCP16x16_2 = Arrays.asList(99, 64, 64, 63, 477, 149, 110, 161, 207);
            PuzzleAlgorithmProperty propertyCP16x16_2 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseCP16x16_2);
            costEstimationsConstants.put("CP_16x16_02", propertyCP16x16_2);

            /*CP_16X16_03*/
            List<Integer> averageIncreaseCP16x16_3 = Arrays.asList(92, 55, 57, 76, 52, 70, 53, 58, 27);
            PuzzleAlgorithmProperty propertyCP16x16_3 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseCP16x16_3);
            costEstimationsConstants.put("CP_16x16_03", propertyCP16x16_3);

            /*CP_16X16_04*/
            List<Integer> averageIncreaseCP16x16_4 = Arrays.asList(84, 55, 57, 57, 50, 54, 48, 123, 20);
            PuzzleAlgorithmProperty propertyCP16x16_4 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseCP16x16_4);
            costEstimationsConstants.put("CP_16x16_04", propertyCP16x16_4);

            /*CP_16X16_05*/
            List<Integer> intervalLimits25x25 = Arrays.asList(50, 100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 625);
            List<Integer> adjustments25x25 = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
            List<Integer> averageIncreaseCP16x16_5 = Arrays.asList(209, 136, 157, 148, 692, 312, 181, 193, 222, 276, 234, 325, 219);
            PuzzleAlgorithmProperty propertyCP16x16_5 = new PuzzleAlgorithmProperty(625L, intervalLimits25x25, adjustments25x25, averageIncreaseCP16x16_5);
            costEstimationsConstants.put("CP_16x16_05", propertyCP16x16_5);
            fieldLoadFitter.addInstance(new double[]{25, 0, 3}, (double) 625);

            /*CP_16X16_06*/
            List<Integer> averageIncreaseCP16x16_6 = Arrays.asList(154, 105, 134, 120, 104, 104, 94, 90, 147, 338, 87, 95, 111);
            PuzzleAlgorithmProperty propertyCP16x16_6 = new PuzzleAlgorithmProperty(625L, intervalLimits25x25, adjustments25x25, averageIncreaseCP16x16_6);
            costEstimationsConstants.put("CP_16x16_06", propertyCP16x16_6);

            /*CP_16X16_06*/
            List<Integer> averageIncreaseCP25x25_1 = Arrays.asList(154, 105, 134, 120, 104, 104, 94, 90, 147, 338, 87, 95, 111);
            PuzzleAlgorithmProperty propertyCP25x25_1 = new PuzzleAlgorithmProperty(625L, intervalLimits25x25, adjustments25x25, averageIncreaseCP25x25_1);
            costEstimationsConstants.put("CP_25x25_01", propertyCP25x25_1);

            /*BFS_9X9_101*/
            List<Integer> averageIncreaseBFSx9_1 = Arrays.asList(25, 28, 34, 17, 29, 20, 25, 38);
            PuzzleAlgorithmProperty propertyBFSx9_1 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseBFSx9_1);
            costEstimationsConstants.put("BFS_9x9_101", propertyBFSx9_1);
            fieldLoadFitter.addInstance(new double[]{9, 0, 1}, (double) 81);

            /*BFS_9X9_102*/
            List<Integer> averageIncreaseBFSx9_2 = Arrays.asList(44, 52, -17, 18, 26, 21, 20, 40);
            PuzzleAlgorithmProperty propertyBFSx9_2 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseBFSx9_2);
            costEstimationsConstants.put("BFS_9x9_102", propertyBFSx9_2);

            /*BFS_9X9_103*/
            List<Integer> averageIncreaseBFSx9_3 = Arrays.asList(26, 49, 23, 19, 75, 20, 24, 45);
            PuzzleAlgorithmProperty propertyBFSx9_3 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseBFSx9_3);
            costEstimationsConstants.put("BFS_9x9_103", propertyBFSx9_3);

            /*BFS_9X9_104*/
            List<Integer> averageIncreaseBFSx9_4 = Arrays.asList(20, 31, 12, 17, 27, 20, 22, 37);
            PuzzleAlgorithmProperty propertyBFSx9_4 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseBFSx9_4);
            costEstimationsConstants.put("BFS_9x9_104", propertyBFSx9_4);

            /*BFS_9X9_105*/
            List<Integer> averageIncreaseBFSx9_5 = Arrays.asList(18, 29, 13, 20, 30, 19, 29, 46);
            PuzzleAlgorithmProperty propertyBFSx9_5 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseBFSx9_5);
            costEstimationsConstants.put("BFS_9x9_105", propertyBFSx9_5);

            /*BFS_16X16_01*/
            List<Integer> averageIncreaseBFS16x16_1 = Arrays.asList(52, 50, 50, 42, 41, 68, 63, 68, 27);
            PuzzleAlgorithmProperty propertyBFS16x16_1 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseBFS16x16_1);
            costEstimationsConstants.put("BFS_16x16_01", propertyBFS16x16_1);
            fieldLoadFitter.addInstance(new double[]{16, 0, 1}, (double) 256);

            /*BFS_16X16_02*/
            List<Integer> averageIncreaseBFS16x16_2 = Arrays.asList(68, 37, 46, 83, 39, 84, 49, 100, 462);
            PuzzleAlgorithmProperty propertyBFS16x16_2 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseBFS16x16_2);
            costEstimationsConstants.put("BFS_16x16_02", propertyBFS16x16_2);

            /*BFS_16X16_03*/
            List<Integer> averageIncreaseBFS16x16_3 = Arrays.asList(49, 38, 41, 56, 45, 233, 57, 70, 30);
            PuzzleAlgorithmProperty propertyBFS16x16_3 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseBFS16x16_3);
            costEstimationsConstants.put("BFS_16x16_03", propertyBFS16x16_3);

            /*BFS_16X16_04*/
            List<Integer> averageIncreaseBFS16x16_4 = Arrays.asList(46, 36, 42, 48, 41, 56, 54, 214, 29);
            PuzzleAlgorithmProperty propertyBFS16x16_4 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseBFS16x16_4);
            costEstimationsConstants.put("BFS_16x16_04", propertyBFS16x16_4);

            /*BFS_16X16_05*/
            List<Integer> averageIncreaseBFS16x16_5 = Arrays.asList(68, 0, 0, 0, 0, 95, 0, 0, 0, 1, 58, 1, 0);
            PuzzleAlgorithmProperty propertyBFS16x16_5 = new PuzzleAlgorithmProperty(625L, intervalLimits25x25, adjustments25x25, averageIncreaseBFS16x16_5);
            costEstimationsConstants.put("BFS_16x16_05", propertyBFS16x16_5);
            fieldLoadFitter.addInstance(new double[]{25, 0, 1}, (double) 625);

            /*BFS_16X16_06*/
            List<Integer> averageIncreaseBFS16x16_6 = Arrays.asList(97, 83, 131, 93, 77, 95, 97, 102, 116, 212, 102, 127, 165);
            PuzzleAlgorithmProperty propertyBFS16x16_6 = new PuzzleAlgorithmProperty(625L, intervalLimits25x25, adjustments25x25, averageIncreaseBFS16x16_6);
            costEstimationsConstants.put("BFS_16x16_06", propertyBFS16x16_6);

            /*BFS_25X25_01*/
            List<Integer> averageIncreaseBFS25x25_1 = Arrays.asList(97, 83, 131, 93, 77, 95, 97, 102, 116, 212, 102, 127, 165);
            PuzzleAlgorithmProperty propertyBFS25x25_1 = new PuzzleAlgorithmProperty(625L, intervalLimits25x25, adjustments25x25, averageIncreaseBFS25x25_1);
            costEstimationsConstants.put("BFS_25x25_01", propertyBFS25x25_1);

            /*DLX_9X9_101*/
            List<Integer> averageIncreaseDLX9x9_1 = Arrays.asList(0, 0, 18, 9, 0, 9, 0, 16);
            PuzzleAlgorithmProperty propertyDLX9x9_1 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseDLX9x9_1);
            costEstimationsConstants.put("DLX_9x9_101", propertyDLX9x9_1);
            fieldLoadFitter.addInstance(new double[]{9, 0, 2}, (double) 81);

            /*DLX_9X9_102*/
            List<Integer> averageIncreaseDLX9x9_2 = Arrays.asList(0, 0, 18, 9, 0, 9, 9, 9);
            PuzzleAlgorithmProperty propertyDLX9x9_2 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseDLX9x9_2);
            costEstimationsConstants.put("DLX_9x9_102", propertyDLX9x9_2);

            /*DLX_9X9_103*/
            List<Integer> averageIncreaseDLX9x9_3 = Arrays.asList(0, 9, 9, 9, 18, 9, 9, 9);
            PuzzleAlgorithmProperty propertyDLX9x9_3 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseDLX9x9_3);
            costEstimationsConstants.put("DLX_9x9_103", propertyDLX9x9_3);

            /*DLX_9X9_104*/
            List<Integer> averageIncreaseDLX9x9_4 = Arrays.asList(0, 0, 0, 0, 0, 0, 27, 0);
            PuzzleAlgorithmProperty propertyDLX9x9_4 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseDLX9x9_4);
            costEstimationsConstants.put("DLX_9x9_104", propertyDLX9x9_4);

            /*DLX_9X9_105*/
            List<Integer> averageIncreaseDLX9x9_5 = Arrays.asList(0, 0, 9, 0, 27, 0, 28, 25);
            PuzzleAlgorithmProperty propertyDLX9x9_5 = new PuzzleAlgorithmProperty(81L, intervalLimits9x9, adjustments9x9_1, averageIncreaseDLX9x9_5);
            costEstimationsConstants.put("DLX_9x9_105", propertyDLX9x9_5);

            /*DLX_16X16_01*/
            List<Integer> averageIncreaseDLX16x16_1 = Arrays.asList(6, 12, 21, 12, 15, 28, 13, 13, 14);
            PuzzleAlgorithmProperty propertyDLX16x16_1 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseDLX16x16_1);
            costEstimationsConstants.put("DLX_16x16_01", propertyDLX16x16_1);
            fieldLoadFitter.addInstance(new double[]{16, 0, 1}, (double) 256);

            /*DLX_16X16_02*/
            List<Integer> averageIncreaseDLX16x16_2 = Arrays.asList(12, 6, 3, 27, 15, 32, 19, 28, 11);
            PuzzleAlgorithmProperty propertyDLX16x16_2 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseDLX16x16_2);
            costEstimationsConstants.put("DLX_16x16_02", propertyDLX16x16_2);

            /*DLX_16X16_03*/
            List<Integer> averageIncreaseDLX16x16_3 = Arrays.asList(0, 6, 12, 21, 15, 28, 25, 15, 11);
            PuzzleAlgorithmProperty propertyDLX16x16_3 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseDLX16x16_3);
            costEstimationsConstants.put("DLX_16x16_03", propertyDLX16x16_3);

            /*DLX_16X16_04*/
            List<Integer> averageIncreaseDLX16x16_4 = Arrays.asList(0, 15, 12, 9, 12, 34, 6, 28, 3);
            PuzzleAlgorithmProperty propertyDLX16x16_4 = new PuzzleAlgorithmProperty(256L, intervalLimits16x16, adjustments16x16, averageIncreaseDLX16x16_4);
            costEstimationsConstants.put("DLX_16x16_04", propertyDLX16x16_4);

            /*DLX_16X16_05*/
            List<Integer> averageIncreaseDLX16x16_5 = Arrays.asList(1, 3, 1, 5, 7, 5, 4, 3, 7, 7, 5, 5, 10);
            PuzzleAlgorithmProperty propertyDLX16x16_5 = new PuzzleAlgorithmProperty(625L, intervalLimits25x25, adjustments25x25, averageIncreaseDLX16x16_5);
            costEstimationsConstants.put("DLX_16x16_05", propertyDLX16x16_5);
            fieldLoadFitter.addInstance(new double[]{25, 0, 2}, (double) 625);

            /*DLX_16X16_06*/
            List<Integer> averageIncreaseDLX16x16_6 = Arrays.asList(0, 7, 7, 4, 6, 7, 13, 7, 11, 17, 13, 2, 18);
            PuzzleAlgorithmProperty propertyDLX16x16_6 = new PuzzleAlgorithmProperty(625L, intervalLimits25x25, adjustments25x25, averageIncreaseDLX16x16_6);
            costEstimationsConstants.put("DLX_16x16_06", propertyDLX16x16_6);

            /*DLX_25X25_01*/
            List<Integer> averageIncreaseDLX25x25_1 = Arrays.asList(0, 7, 7, 4, 6, 7, 13, 7, 11, 17, 13, 2, 18);
            PuzzleAlgorithmProperty propertyDLX25x25_1 = new PuzzleAlgorithmProperty(625L, intervalLimits25x25, adjustments25x25, averageIncreaseDLX25x25_1);
            costEstimationsConstants.put("DLX_25x25_01", propertyDLX25x25_1);



        } catch(Exception e) {
            e.printStackTrace();
        }

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
        String[] queryAttributes = query.split("&");
        String algorithmName = queryAttributes[0].split("=")[1];

        String[] puzzle = queryAttributes[4].split("=")[1].split("_");
        String puzzleName = puzzle[2] + "_" + puzzle[3];

        return algorithmName + "_" + puzzleName;
    }

    public static Integer extractUnassigned(String query) {
        String[] queryAttributes = query.split("&");

        return Integer.parseInt(queryAttributes[1].split("=")[1]);
    }
}