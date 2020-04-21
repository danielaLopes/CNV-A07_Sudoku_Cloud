package BIT.serverMetrics;

import BIT.highBIT.*;

import java.io.*;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MetricsTool {

    private static ConcurrentMap<Long, Long> methodCount = new ConcurrentHashMap<>();

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
                    routine.addBefore("BIT/serverMetrics/MetricsTool", "methodCount", new Integer(1));
                }
                ci.write(out_filename);
            }
        }
    }

    public static synchronized void printToFile(String query)
    {
        Long currentThreadId = Thread.currentThread().getId();
        Long current_method_count = methodCount.get(currentThreadId);
        resetCount(currentThreadId);

        File directory = new File("logs/");
        if (! directory.exists()){
            directory.mkdir();
        }
        //new File("logs/serverMetrics.csv");
        try {
            FileWriter fileWriter = new FileWriter("logs/serverMetrics.txt", true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter out = new PrintWriter(bufferedWriter);
            out.println("Query: " + query);
            out.println("Number of methods: " + current_method_count);
            out.println("-------------------------------------------");
            out.close();
            bufferedWriter.close();
            fileWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred while printing to file.");
            e.printStackTrace();
        }
    }

    public static void resetCount(long currentThreadId)
    {
        methodCount.put(currentThreadId, 0L);
    }

    public static synchronized void methodCount(int incr)
    {
        Long currentThreadId = Thread.currentThread().getId();

        Long current_method_count = methodCount.get(currentThreadId);
        if (current_method_count == null) current_method_count  = 0l;

        methodCount.put(currentThreadId, current_method_count  + 1);
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
