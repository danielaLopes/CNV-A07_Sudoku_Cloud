/* ICount.java
* Sample program using BIT -- counts the number of instructions executed.
*
* Copyright (c) 1997, The Regents of the University of Colorado. All
* Rights Reserved.
*
* Permission to use and copy this software and its documentation for
* NON-COMMERCIAL purposes and without fee is hereby granted provided
* that this copyright notice appears in all copies. If you wish to use
* or wish to have others use BIT for commercial purposes please contact,
* Stephen V. O'Neil, Director, Office of Technology Transfer at the
* University of Colorado at Boulder (303) 492-5647.
*/

import BIT.highBIT.*;
import java.io.*;
import java.util.*;

import java.io.File;  // Import the File class
import java.io.IOException;  // Import the IOException class to handle errors
import java.io.FileWriter;   // Import the FileWriter class


public class ICount {
    private static PrintStream out = null;
    //private static int i_count_s = 0, b_count_s = 0, m_count_s = 0;
    private static Map<Long, Integer> i_count = new HashMap<>();
    private static Map<Long, Integer> b_count = new HashMap<>();
    private static Map<Long, Integer> m_count = new HashMap<>();

    /* main reads in all the files class files present in the input directory,
     * instruments them, and outputs them to the specified output directory.
     */
    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        String infilenames[] = file_in.list();

        try {
            File myObj = new File("/home/daniela/daniela/CNV/BIT/examples/pt_output/log.txt");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }


        for (int i = 0; i < infilenames.length; i++) {
            String infilename = infilenames[i];
            //System.out.println("filename: " + infilename);

            if (infilename.endsWith(".class")) {
                // create class info object
                ClassInfo ci = new ClassInfo(argv[0] + System.getProperty("file.separator") + infilename);

                // loop through all the routines
                // see java.util.Enumeration for more information on Enumeration class
                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {

                    Routine routine = (Routine) e.nextElement();
                    routine.addBefore("ICount", "mcount", new Integer(1));

                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        bb.addBefore("ICount", "count", new Integer(bb.size()));
                    }
                    //System.out.println("routine: " + routine.getMethodName());
                    if (routine.getMethodName().equals("solveSudoku")) {
                        //System.out.println("Last element!");
                        routine.addAfter("ICount", "printICount", ci.getClassName());
                    }
                    //ci.addAfter("ICount", "printICount", ci.getClassName());
                }
                ci.write(argv[1] + System.getProperty("file.separator") + infilename);
            }
        }
    }

    public static synchronized void printICount(String className) {
        Long currentThreadId = Thread.currentThread().getId();
        try {
            FileWriter myWriter = new FileWriter("/home/daniela/daniela/CNV/BIT/examples/pt_output/ICount_log.txt", true);

            // print results and put counters back to zero
            myWriter.write(i_count.get(currentThreadId) + " instructions in thread" + currentThreadId + '\n');
            i_count.put(currentThreadId, 0);

            myWriter.write(b_count.get(currentThreadId) + " basic blocks in thread" + currentThreadId + '\n');
            b_count.put(currentThreadId, 0);

            myWriter.write(m_count.get(currentThreadId) + " methods in thread" + currentThreadId + '\n');
            m_count.put(currentThreadId, 0);

            myWriter.write("-------------------------------------------------" + '\n');
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


    public static synchronized void count(int incr) {
        Long currentThreadId = Thread.currentThread().getId();
        Integer currentICount = i_count.get(currentThreadId);
        if (currentICount == null) currentICount = 0;
        Integer currentBCount = b_count.get(currentThreadId);
        if (currentBCount == null) currentBCount = 0;

        i_count.put(currentThreadId, currentICount + incr);
        b_count.put(currentThreadId, ++currentBCount);
    }

    public static synchronized void mcount(int incr) {
        Long currentThreadId = Thread.currentThread().getId();
        Integer currentMCount = m_count.get(currentThreadId);
        if (currentMCount == null) currentMCount = 0;
        m_count.put(currentThreadId, ++currentMCount);
    }
}