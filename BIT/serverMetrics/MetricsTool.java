package BIT.serverMetrics;

import BIT.highBIT.*;

import java.io.*;
import java.util.*;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.util.TableUtils;

public class MetricsTool {
    private static AmazonDynamoDB dynamoDB;

    private static Map<Long, Long> fieldloadcount = new HashMap<>();

    public static void printUsage()
    {
        System.out.println("Syntax: java MetricsTool in_path [out_path]");

        System.out.println("        in_path:  directory from which the class files are read");
        System.out.println("        out_path: directory to which the class files are written");
        System.out.println("        Both in_path and out_path are required");
        System.exit(-1);
    }

    public static void doCount(File in_dir, File out_dir)
    {
        String filelist[] = in_dir.list();

        for (int i = 0; i < filelist.length; i++) {
            String filename = filelist[i];
            if (filename.endsWith(".class")) {
                String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                ClassInfo ci = new ClassInfo(in_filename);

                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();

                    for (Enumeration instrs = (routine.getInstructionArray()).elements(); instrs.hasMoreElements(); ) {
                        Instruction instr = (Instruction) instrs.nextElement();
                        int opcode = instr.getOpcode();
                        if (opcode == InstructionTable.getfield)
                            instr.addBefore("MetricsTool", "count", new Integer(0));
                    }
                }
                ci.write(out_filename);
            }
        }
    }

    public static synchronized void printToFile(String query)
    {
        Long currentThreadId = Thread.currentThread().getId();
        Long current_field_load_count = fieldloadcount.get(currentThreadId);
        resetCount(currentThreadId);

        File directory = new File("/home/ec2-user/logs/");
        if (! directory.exists()){
            directory.mkdir();
        }
        
        try {
            FileWriter fileWriter = new FileWriter("/home/ec2-user/logs/serverMetrics.txt", true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter out = new PrintWriter(bufferedWriter);
            out.println("Query: " + query);
            out.println("Number of field load: " + current_field_load_count);
            out.println("-------------------------------------------");
            out.close();
            bufferedWriter.close();
            fileWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred while printing to file.");
            e.printStackTrace();
        }
    }

    public static synchronized void insertDynamo(String query)
    {
        Long currentThreadId = Thread.currentThread().getId();
        Long current_field_load_count = fieldloadcount.get(currentThreadId);
        resetCount(currentThreadId);

        ProfileCredentialsProvider credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
            System.out.println("found credentials");
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (~/.aws/credentials), and is in valid format.",
                    e);
        }

        dynamoDB = AmazonDynamoDBClientBuilder.standard()
            .withCredentials(credentialsProvider)
            .withRegion("us-east-1")
            .build();

        try {
            String tableName = "field-loads-table";

            // primary key => requestID, which holds a string characteristic of each request
            CreateTableRequest createTableRequest = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement().withAttributeName("requestID").withKeyType(KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition().withAttributeName("requestID").withAttributeType(ScalarAttributeType.S))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(1L).withWriteCapacityUnits(1L));

            // Create table if it does not exist yet
            TableUtils.createTableIfNotExists(dynamoDB, createTableRequest);
            // wait for the table to move into ACTIVE state
            TableUtils.waitUntilActive(dynamoDB, tableName);

            // Describe our new table
            DescribeTableRequest describeTableRequest = new DescribeTableRequest().withTableName(tableName);
            TableDescription tableDescription = dynamoDB.describeTable(describeTableRequest).getTable();
            System.out.println("Table Description: " + tableDescription);

            // Add an item
            Map<String, AttributeValue> item = newItem(query, current_field_load_count);
            PutItemRequest putItemRequest = new PutItemRequest(tableName, item);
            PutItemResult putItemResult = dynamoDB.putItem(putItemRequest);
            System.out.println("Result: " + putItemResult);

        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to AWS, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with AWS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch(InterruptedException ie) {
            System.out.println("Thread was interrupted.");
        }

        
    }

    public static void resetCount(long currentThreadId)
    {
        fieldloadcount.put(currentThreadId, 0L);
    }

    public static synchronized void count(int incr)
    {
        Long currentThreadId = Thread.currentThread().getId();

        Long current_field_load_count = fieldloadcount.get(currentThreadId);
        if (current_field_load_count == null) current_field_load_count  = 0l;

        fieldloadcount.put(currentThreadId, current_field_load_count  + 1);
    }

    private static Map<String, AttributeValue> newItem(String query, long fieldLoads) {
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put("requestID", new AttributeValue(query));
        item.put("fieldLoads", new AttributeValue().withN(Long.toString(fieldLoads)));
        
        return item;
    }
    
    public static void main(String argv[])
    {
        if (argv.length < 2) {
            printUsage();
        }

        try {
            File in_dir = new File(argv[0]);
            File out_dir = new File(argv[1]);

            if (in_dir.isDirectory() && out_dir.isDirectory()) {

                doCount(in_dir, out_dir);
            }
            else {
                printUsage();
            }
        }
        catch (NullPointerException e) {
            printUsage();
        }
    }
}