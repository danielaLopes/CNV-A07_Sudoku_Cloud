/* 
.java
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
import java.util.*;

import java.io.File;  // Import the File class
import java.io.IOException;  // Import the IOException class to handle errors

public class ICount {

    /* main reads in all the files class files present in the input directory,
     * instruments them, and outputs them to the specified output directory.
     */
    public static void main(String argv[]) {
        File file_in = new File(argv[0]);
        String infilenames[] = file_in.list();

        try {
            File myObj = new File("requests_instrumentation.txt");
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
                    routine.addBefore("pt/ulisboa/tecnico/cnv/server/WebServer", "mcount", new Integer(1));

                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        bb.addBefore("pt/ulisboa/tecnico/cnv/server/WebServer", "count", new Integer(bb.size()));
                    }
                }
                ci.write(argv[1] + System.getProperty("file.separator") + infilename);
            }
        }
    }
}
