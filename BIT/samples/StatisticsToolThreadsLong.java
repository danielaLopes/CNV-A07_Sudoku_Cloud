//
// StatisticsThreads.java
//
// This program measures and instruments to obtain different statistics
// about Java programs.
//
// Copyright (c) 1998 by Han B. Lee (hanlee@cs.colorado.edu).
// ALL RIGHTS RESERVED.
//
// Permission to use, copy, modify, and distribute this software and its
// documentation for non-commercial purposes is hereby granted provided
// that this copyright notice appears in all copies.
//
// This software is provided "as is".  The licensor makes no warrenties, either
// expressed or implied, about its correctness or performance.  The licensor
// shall not be liable for any damages suffered as a result of using
// and modifying this software.

import BIT.highBIT.*;
import BIT.lowBIT.Method_Info;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class StatisticsToolThreadsLong
{
    private static Map<Long, Long> dyn_method_count = new HashMap<>();
    private static Map<Long, Long> dyn_bb_count = new HashMap<>();
    private static Map<Long, Long> dyn_instr_count = new HashMap<>();

    private static Map<Long, Long> newcount = new HashMap<>();
    private static Map<Long, Long> newarraycount = new HashMap<>();
    private static Map<Long, Long> anewarraycount = new HashMap<>();
    private static Map<Long, Long> multianewarraycount = new HashMap<>();

    private static Map<Long, Long> loadcount = new HashMap<>();
    private static Map<Long, Long> storecount = new HashMap<>();
    private static Map<Long, Long> fieldloadcount = new HashMap<>();
    private static Map<Long, Long> fieldstorecount = new HashMap<>();

    private static StatisticsBranchThreads[] branch_info;
    private static int branch_number;
    private static int branch_pc;
    private static String branch_class_name;
    private static String branch_method_name;

    public static void printUsage()
    {
        System.out.println("Syntax: java StatisticsThreads -stat_type in_path [out_path]");
        System.out.println("        where stat_type can be:");
        System.out.println("        static:     static properties");
        System.out.println("        dynamic:    dynamic properties");
        System.out.println("        alloc:      memory allocation instructions");
        System.out.println("        load_store: loads and stores (both field and regular)");
        System.out.println("        branch:     gathers branch outcome statistics");
        System.out.println();
        System.out.println("        in_path:  directory from which the class files are read");
        System.out.println("        out_path: directory to which the class files are written");
        System.out.println("        Both in_path and out_path are required unless stat_type is static");
        System.out.println("        in which case only in_path is required");
        System.exit(-1);
    }

    public static void doStatic(File in_dir)
    {
        String filelist[] = in_dir.list();
        int method_count = 0;
        int bb_count = 0;
        int instr_count = 0;
        int class_count = 0;

        for (int i = 0; i < filelist.length; i++) {
            String filename = filelist[i];
            if (filename.endsWith(".class")) {
                class_count++;
                String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                ClassInfo ci = new ClassInfo(in_filename);
                Vector routines = ci.getRoutines();
                method_count += routines.size();

                for (Enumeration e = routines.elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    BasicBlockArray bba = routine.getBasicBlocks();
                    bb_count += bba.size();
                    InstructionArray ia = routine.getInstructionArray();
                    instr_count += ia.size();
                }
            }
        }

        System.out.println("Static information summary:");
        System.out.println("Number of class files:  " + class_count);
        System.out.println("Number of methods:      " + method_count);
        System.out.println("Number of basic blocks: " + bb_count);
        System.out.println("Number of instructions: " + instr_count);

        if (class_count == 0 || method_count == 0) {
            return;
        }

        float instr_per_bb = (float) instr_count / (float) bb_count;
        float instr_per_method = (float) instr_count / (float) method_count;
        float instr_per_class = (float) instr_count / (float) class_count;
        float bb_per_method = (float) bb_count / (float) method_count;
        float bb_per_class = (float) bb_count / (float) class_count;
        float method_per_class = (float) method_count / (float) class_count;

        System.out.println("Average number of instructions per basic block: " + instr_per_bb);
        System.out.println("Average number of instructions per method:      " + instr_per_method);
        System.out.println("Average number of instructions per class:       " + instr_per_class);
        System.out.println("Average number of basic blocks per method:      " + bb_per_method);
        System.out.println("Average number of basic blocks per class:       " + bb_per_class);
        System.out.println("Average number of methods per class:            " + method_per_class);
    }

    public static void doDynamic(File in_dir, File out_dir)
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
                    routine.addBefore("StatisticsToolThreadsLong", "dynMethodCount", new Integer(1));

                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        bb.addBefore("StatisticsToolThreadsLong", "dynInstrCount", new Integer(bb.size()));
                    }
                    if (routine.getMethodName().equals("solveSudoku")) {
                        //System.out.println("Last element!");
                        routine.addAfter("StatisticsToolThreadsLong", "printDynamic", ci.getClassName());
                    }
                }
                //ci.addAfter("StatisticsToolsThreads", "printDynamic", "null");
                ci.write(out_filename);
            }
        }
    }

    public static synchronized void printDynamic(String foo)
    {
        Long currentThreadId = Thread.currentThread().getId();

        try {
            FileWriter myWriter = new FileWriter("log.txt", true);

            Long current_dyn_method_count = dyn_method_count.get(currentThreadId);
            Long current_dyn_bb_count = dyn_bb_count.get(currentThreadId);
            Long current_dyn_instr_count = dyn_instr_count.get(currentThreadId);

            // put back to zero
            dyn_method_count.put(currentThreadId, 0L);
            dyn_bb_count.put(currentThreadId, 0L);
            dyn_instr_count.put(currentThreadId, 0L);

            myWriter.write("Dynamic information summary thread " + currentThreadId + ":\n");
            myWriter.write("Number of methods:      " + current_dyn_method_count + "\n");
            myWriter.write("Number of basic blocks: " + current_dyn_bb_count + "\n");
            myWriter.write("Number of instructions: " + current_dyn_instr_count + "\n");

            if (current_dyn_method_count == 0) {
                return;
            }

            float instr_per_bb = (float) current_dyn_instr_count / (float) current_dyn_bb_count;
            float instr_per_method = (float) current_dyn_instr_count / (float) current_dyn_method_count;
            float bb_per_method = (float) current_dyn_bb_count / (float) current_dyn_method_count;

            myWriter.write("Average number of instructions per basic block: " + instr_per_bb + "\n");
            myWriter.write("Average number of instructions per method:      " + instr_per_method + "\n");
            myWriter.write("Average number of basic blocks per method:      " + bb_per_method + "\n");

            myWriter.write("-------------------------------------------------" + '\n');
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }


    public static synchronized void dynInstrCount(int incr)
    {
        Long currentThreadId = Thread.currentThread().getId();

        Long currentDynInstrCount = dyn_instr_count.get(currentThreadId);
        if (currentDynInstrCount == null) currentDynInstrCount = 0l;
        Long currentDynBbCount = dyn_bb_count.get(currentThreadId);
        if (currentDynBbCount == null) currentDynBbCount = 0l;

        dyn_instr_count.put(currentThreadId, currentDynInstrCount + incr);
        dyn_bb_count.put(currentThreadId, currentDynBbCount + 1);
    }

    public static synchronized void dynMethodCount(int incr)
    {
        Long currentThreadId = Thread.currentThread().getId();

        Long currentDynMethodCount = dyn_method_count.get(currentThreadId);
        if (currentDynMethodCount == null) currentDynMethodCount = 0l;

        dyn_method_count.put(currentThreadId, currentDynMethodCount + 1);
    }

    public static void doAlloc(File in_dir, File out_dir)
    {
        String filelist[] = in_dir.list();

        for (int i = 0; i < filelist.length; i++) {
            String filename = filelist[i];
            if (filename.endsWith(".class")) {
                String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                ClassInfo ci = new ClassInfo(in_filename);

                Vector routines = ci.getRoutines();

                for (Enumeration e = routines.elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    InstructionArray instructions = routine.getInstructionArray();

                    for (Enumeration instrs = instructions.elements(); instrs.hasMoreElements(); ) {
                        Instruction instr = (Instruction) instrs.nextElement();
                        int opcode=instr.getOpcode();
                        if ((opcode==InstructionTable.NEW) ||
                                (opcode==InstructionTable.newarray) ||
                                (opcode==InstructionTable.anewarray) ||
                                (opcode==InstructionTable.multianewarray)) {
                            instr.addBefore("StatisticsToolThreadsLong", "allocCount", new Integer(opcode));
                        }
                    }
                    if (routine.getMethodName().equals("solveSudoku")) {
                        System.out.println("Last element!");
                        routine.addAfter("StatisticsToolThreadsLong", "printAlloc", ci.getClassName());
                    }
                }
                //ci.addAfter("StatisticsToolThreadsLong", "printAlloc", "null");
                ci.write(out_filename);
            }
        }
    }

    public static synchronized void printAlloc(String s)
    {
        long currentThreadId = Thread.currentThread().getId();
        try {
            FileWriter myWriter = new FileWriter("log.txt",true);

            // print results and put counters back to zero
            myWriter.write("Allocations summary for thread " + currentThreadId + ":\n");

            myWriter.write("new:            " + newcount.get(currentThreadId) + "\n");
            newcount.put(currentThreadId, 0l);

            myWriter.write("newarray:       " + newarraycount.get(currentThreadId) + "\n");
            newarraycount.put(currentThreadId, 0l);

            myWriter.write("anewarray:      " + anewarraycount.get(currentThreadId) + "\n");
            anewarraycount.put(currentThreadId, 0l);

            myWriter.write("multianewarray: " + multianewarraycount.get(currentThreadId) + "\n");
            multianewarraycount.put(currentThreadId, 0l);


            myWriter.write("-------------------------------------------------" + "\n");
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static synchronized void allocCount(int type)
    {
        long currentThreadId = Thread.currentThread().getId();
        Long current_newcount = newcount.get(currentThreadId);
        if (current_newcount == null) {
            current_newcount = 0l;
            newcount.put(currentThreadId, 0l);
        }
        Long current_newarraycount = newarraycount.get(currentThreadId);
        if (current_newarraycount == null) {
            current_newarraycount = 0l;
            newarraycount.put(currentThreadId, 0l);
        }
        Long current_anewarraycount = anewarraycount.get(currentThreadId);
        if (current_anewarraycount == null) {
            current_anewarraycount = 0l;
            anewarraycount.put(currentThreadId, 0l);
        }
        Long current_multianewarraycount = multianewarraycount.get(currentThreadId);
        if (current_multianewarraycount == null) {
            current_multianewarraycount = 0l;
            multianewarraycount.put(currentThreadId, 0l);
        }

        switch(type) {
            case InstructionTable.NEW:
                newcount.put(currentThreadId, current_newcount + 1);
                break;
            case InstructionTable.newarray:
                newarraycount.put(currentThreadId, current_newarraycount + 1);
                break;
            case InstructionTable.anewarray:
                anewarraycount.put(currentThreadId, current_anewarraycount + 1);
                break;
            case InstructionTable.multianewarray:
                multianewarraycount.put(currentThreadId, current_multianewarraycount + 1);
                break;
        }
    }

    public static void doLoadStore(File in_dir, File out_dir)
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
                        int opcode=instr.getOpcode();
                        if (opcode == InstructionTable.getfield)
                            instr.addBefore("StatisticsToolThreadsLong", "LSFieldCount", new Integer(0));
                        else if (opcode == InstructionTable.putfield)
                            instr.addBefore("StatisticsToolThreadsLong", "LSFieldCount", new Integer(1));
                        else {
                            short instr_type = InstructionTable.InstructionTypeTable[opcode];
                            if (instr_type == InstructionTable.LOAD_INSTRUCTION) {
                                instr.addBefore("StatisticsToolThreadsLong", "LSCount", new Integer(0));
                            } else if (instr_type == InstructionTable.STORE_INSTRUCTION) {
                                instr.addBefore("StatisticsToolThreadsLong", "LSCount", new Integer(1));
                            }
                        }
                    }
                    if (routine.getMethodName().equals("solveSudoku")) {
                        //System.out.println("Last element!");
                        routine.addAfter("StatisticsToolThreadsLong", "printLoadStore", ci.getClassName());
                    }
                }
                //ci.addAfter("StatisticsToolThreadsLong", "printLoadStore", "null");
                ci.write(out_filename);
            }
        }
    }

    public static synchronized void printLoadStore(String s)
    {
        Long currentThreadId = Thread.currentThread().getId();
        try {
            FileWriter myWriter = new FileWriter("log_load_store.txt", true);

            // print results and put counters back to zero
            myWriter.write("Load Store Summary in thread" + currentThreadId + ":\n");
            myWriter.write("Field load:    " + fieldloadcount.get(currentThreadId) + "\n");
            fieldloadcount.put(currentThreadId, 0l);

            myWriter.write("Field store:   " + fieldstorecount.get(currentThreadId) + "\n");
            fieldstorecount.put(currentThreadId, 0l);

            myWriter.write("Regular load:  " + loadcount.get(currentThreadId) + "\n");
            loadcount.put(currentThreadId, 0l);

            myWriter.write("Regular store: " + storecount.get(currentThreadId) + "\n");
            storecount.put(currentThreadId, 0l);

            myWriter.write("-------------------------------------------------" + "\n");
            myWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public static synchronized void LSFieldCount(int type)
    {
        Long currentThreadId = Thread.currentThread().getId();

        Long currentFieldLoadCount = fieldloadcount.get(currentThreadId);
        if (currentFieldLoadCount == null) currentFieldLoadCount = 0l;
        Long currentFieldStoreCount = fieldstorecount.get(currentThreadId);
        if (currentFieldStoreCount == null) currentFieldStoreCount = 0l;

        if (type == 0) {
            fieldloadcount.put(currentThreadId, currentFieldLoadCount + 1);
        }
        else {
            fieldstorecount.put(currentThreadId, currentFieldStoreCount + 1);
        }
    }

    public static synchronized void LSCount(int type)
    {
        Long currentThreadId = Thread.currentThread().getId();

        Long currentLoadCount = loadcount.get(currentThreadId);
        if (currentLoadCount == null) currentLoadCount = 0l;
        Long currentStoreCount = storecount.get(currentThreadId);
        if (currentStoreCount == null) currentStoreCount = 0l;

        if (type == 0) {
            loadcount.put(currentThreadId, currentLoadCount + 1);
        }
        else {
            storecount.put(currentThreadId, currentStoreCount + 1);
        }
    }

    public static void doBranch(File in_dir, File out_dir)
    {
        String filelist[] = in_dir.list();
        int k = 0;
        int total = 0;

        for (int i = 0; i < filelist.length; i++) {
            String filename = filelist[i];
            if (filename.endsWith(".class")) {
                String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                ClassInfo ci = new ClassInfo(in_filename);

                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    InstructionArray instructions = routine.getInstructionArray();
                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        Instruction instr = (Instruction) instructions.elementAt(bb.getEndAddress());
                        short instr_type = InstructionTable.InstructionTypeTable[instr.getOpcode()];
                        if (instr_type == InstructionTable.CONDITIONAL_INSTRUCTION) {
                            total++;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < filelist.length; i++) {
            String filename = filelist[i];
            if (filename.endsWith(".class")) {
                String in_filename = in_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                String out_filename = out_dir.getAbsolutePath() + System.getProperty("file.separator") + filename;
                ClassInfo ci = new ClassInfo(in_filename);

                for (Enumeration e = ci.getRoutines().elements(); e.hasMoreElements(); ) {
                    Routine routine = (Routine) e.nextElement();
                    routine.addBefore("StatisticsToolThreadsLong", "setBranchMethodName", routine.getMethodName());
                    InstructionArray instructions = routine.getInstructionArray();
                    for (Enumeration b = routine.getBasicBlocks().elements(); b.hasMoreElements(); ) {
                        BasicBlock bb = (BasicBlock) b.nextElement();
                        Instruction instr = (Instruction) instructions.elementAt(bb.getEndAddress());
                        short instr_type = InstructionTable.InstructionTypeTable[instr.getOpcode()];
                        if (instr_type == InstructionTable.CONDITIONAL_INSTRUCTION) {
                            instr.addBefore("StatisticsToolThreadsLong", "setBranchPC", new Integer(instr.getOffset()));
                            instr.addBefore("StatisticsToolThreadsLong", "updateBranchNumber", new Integer(k));
                            instr.addBefore("StatisticsToolThreadsLong", "updateBranchOutcome", "BranchOutcome");
                            k++;
                        }
                    }
                    if (routine.getMethodName().equals("solveSudoku")) {
                        //System.out.println("Last element!");
                        routine.addAfter("StatisticsToolThreadsLong", "printBranch", ci.getClassName());
                    }
                }
                ci.addBefore("StatisticsToolThreadsLong", "setBranchClassName", ci.getClassName());
                ci.addBefore("StatisticsToolThreadsLong", "branchInit", new Integer(total));
                //ci.addAfter("StatisticsToolThreadsLong", "printBranch", "null");
                ci.write(out_filename);
            }
        }
    }

    public static synchronized void setBranchClassName(String name)
    {
        branch_class_name = name;
    }

    public static synchronized void setBranchMethodName(String name)
    {
        branch_method_name = name;
    }

    public static synchronized void setBranchPC(int pc)
    {
        branch_pc = pc;
    }

    public static synchronized void branchInit(int n)
    {
        if (branch_info == null) {
            branch_info = new StatisticsBranchThreads[n];
        }
    }

    public static synchronized void updateBranchNumber(int n)
    {
        branch_number = n;

        if (branch_info[branch_number] == null) {
            branch_info[branch_number] = new StatisticsBranchThreads(branch_class_name, branch_method_name, branch_pc);
        }
    }

    public static synchronized void updateBranchOutcome(int br_outcome)
    {
        if (br_outcome == 0) {
            branch_info[branch_number].incrNotTaken();
        }
        else {
            branch_info[branch_number].incrTaken();
        }
    }

    public static synchronized void printBranch(String foo)
    {
        System.out.println("Branch summary:");
        System.out.println("CLASS NAME" + '\t' + "METHOD" + '\t' + "PC" + '\t' + "TAKEN" + '\t' + "NOT_TAKEN");

        for (int i = 0; i < branch_info.length; i++) {
            if (branch_info[i] != null) {
                branch_info[i].print();
            }
        }
    }


    public static void main(String argv[])
    {

        try {
            File myObj = new File(
                    "log_load_store.txt");
            if (myObj.createNewFile()) {
                System.out.println("File created: " + myObj.getName());
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }

        
        if (argv.length < 2 || !argv[0].startsWith("-")) {
            printUsage();
        }

        if (argv[0].equals("-static")) {
            if (argv.length != 2) {
                printUsage();
            }

            try {
                File in_dir = new File(argv[1]);

                if (in_dir.isDirectory()) {
                    doStatic(in_dir);
                }
                else {
                    printUsage();
                }
            }
            catch (NullPointerException e) {
                printUsage();
            }
        }

        else if (argv[0].equals("-dynamic")) {
            if (argv.length != 3) {
                printUsage();
            }

            try {
                File in_dir = new File(argv[1]);
                File out_dir = new File(argv[2]);

                if (in_dir.isDirectory() && out_dir.isDirectory()) {
                    
                    doDynamic(in_dir, out_dir);
                }
                else {
                    printUsage();
                }
            }
            catch (NullPointerException e) {
                printUsage();
            }
        }

        else if (argv[0].equals("-alloc")) {
            if (argv.length != 3) {
                printUsage();
            }

            try {
                File in_dir = new File(argv[1]);
                File out_dir = new File(argv[2]);

                if (in_dir.isDirectory() && out_dir.isDirectory()) {
                    
                    doAlloc(in_dir, out_dir);
                }
                else {
                    printUsage();
                }
            }
            catch (NullPointerException e) {
                printUsage();
            }
        }

        else if (argv[0].equals("-load_store")) {
            if (argv.length != 3) {
                printUsage();
            }

            try {
                File in_dir = new File(argv[1]);
                File out_dir = new File(argv[2]);

                if (in_dir.isDirectory() && out_dir.isDirectory()) {
                    
                    doLoadStore(in_dir, out_dir);
                }
                else {
                    printUsage();
                }
            }
            catch (NullPointerException e) {
                printUsage();
            }
        }

        else if (argv[0].equals("-branch")) {
            if (argv.length != 3) {
                printUsage();
            }

            try {
                File in_dir = new File(argv[1]);
                File out_dir = new File(argv[2]);

                if (in_dir.isDirectory() && out_dir.isDirectory()) {
                    
                    doBranch(in_dir, out_dir);
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
}

