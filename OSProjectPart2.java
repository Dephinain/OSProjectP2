package osprojectpart2;
/*
Name: Myles Brown
Course Number: CS 4332
Assignment Title: Operating Systems Project - Phase 2
Date: 4/15/2018
Global Variables: J_SCHED sched - this is the job scheduler class that properly reads, sorts, and appends relevant information to the job based upon its ID, memory need, and class.
				  jobsProcessed - Counts how many jobs have been processed by the program
				  cpuJobCount  - Counts how many cpu-based jobs have been processed
				  ioJobCount - Counts how many io-based jobs have been processed
				  balancedJobCount - Counts how many io/cpu balanced jobs have been processed
				  totalTurnaround - Gets the total turnaround time for all processed jobs
				  totalWaitTime - Gets the total wait time for all processed jobs
				  rejectCount - Counts how many jobs have been rejected due to being either empty jobs (0) or not meeting the memory requirements
				  cpuTime - Keeps track of the 'processing time' that has occurred throughout the program.
				  qCONSTRAINT - The hardcap on memory availability
				  
File description: Main protocol for the scheduler, takes input from the incoming job file and calls appropriate methods and functions (J_SCHED, J_DISPATCH, J_TERM) according to input.

Possible improvements: A pretty bad way of handling the 0 jobs that got loaded to disk and subsequently memory/execution. Checks built in for such a thing don't work properly, and
would require more time than alloted to fix. The previous version I turned in also had some pretty grievous logic errors regarding the actual logic in properly scheduling and running
of jobs.
				  
 */
import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class OSProjectPart2 {

    public static J_SCHED sched = new J_SCHED();
    public static int jobsProcessed;
    public static int cpuJobCount, ioJobCount, balancedJobCount, totalTurnaround, totalWaitTime, rejectCount, totalProcess, cpuTime;
    public static final int qCONSTRAINT = 26;

    //Job termination function. Documents and outputs job termination statistics, and purges the appropriate queues upon termination.
    public static void J_TERM(String finishedJob) 
	{
        long term_time = cpuTime; //Termination time
        long tat_time, wait_time; //Turnaround time and wait time
        String[] term_tokens; //Array to hold to-be tokenized job string
        term_tokens = finishedJob.split("\\s+"); //Tokenizes terminating job string
        if (Integer.parseInt(term_tokens[1]) == 0) {
        } else {
            tat_time = term_time - Math.abs(Long.parseLong(term_tokens[4]) - Long.parseLong(term_tokens[5]));
            wait_time = term_time - Long.parseLong(term_tokens[5]);
            if (Integer.parseInt(term_tokens[2]) == 1) {
                cpuJobCount++;
            } else if (Integer.parseInt(term_tokens[2]) == 2) {
                balancedJobCount++;
            } else if (Integer.parseInt(term_tokens[2]) == 3) {
                ioJobCount++;
            }

            //Entire following brick outputs individual job statistics
            System.out.println("\nJob stats: ");
            System.out.println("Job ID: " + term_tokens[1]);
            System.out.println("Class of Job: " + term_tokens[2]);
            System.out.println("Time job was submitted: " + term_tokens[5] + " milliseconds.");
            System.out.println("Time job was loaded to ready queue: " + term_tokens[6] + " milliseconds.");
            System.out.println("Time job was terminated at: " + term_time + " milliseconds.");
            System.out.println("Time job was spent processing: " + term_tokens[4] + " milliseconds.");
            System.out.println("Memory needed to run: " + term_tokens[3] + " bytes.");
            System.out.println("Turnaround time: " + tat_time + " milliseconds.");
            System.out.println("Waiting time: " + wait_time + " milliseconds.");
            sched.releaseMemory(finishedJob); //Inputs job string to release memory after execution
            totalTurnaround += tat_time;
            totalWaitTime += wait_time;
            jobsProcessed++;
        }

    }

    //Executes/processes incoming job from ready queue
    public static void J_DISPATCH(String arrivingJob) 
	{
        String[] dispatch_tokens; //Array to hold to-be tokenized
        dispatch_tokens = arrivingJob.split("\\s+"); //Tokenizes executing job string
        try {
            cpuTime += Integer.parseInt(dispatch_tokens[4]);
            J_TERM(arrivingJob); //Calls after job 'execution' to output termination stats
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static boolean appendLoadCheck(String incomingJob, ArrayBlockingQueue rQueue)
    {
        StringBuffer appendLoadTime = new StringBuffer(incomingJob);
        String[] tokens;
        tokens = incomingJob.split("\\s+");
        if(sched.aquireMemoryCheck(incomingJob))
        {
            appendLoadTime.append(" " + cpuTime);
            incomingJob = appendLoadTime.toString();
            rQueue.add(incomingJob);
            return true;
        }
        else
            return false;
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        //File file = new File("/home/opsys/OS-I/18Sp-jobs");
        File file = new File("src/osprojectpart2/18Sp-jobs"); //Incoming job file
        String line; //Line that holds incoming job to be checked
        ArrayBlockingQueue<String> readyQueue = new ArrayBlockingQueue<>(qCONSTRAINT); //Ready queue initialized with the constraint
        ArrayBlockingQueue<String> IOqueue = new ArrayBlockingQueue<>(8);
        ArrayBlockingQueue<String> balancedQueue = new ArrayBlockingQueue<>(8);
        ArrayBlockingQueue<String> CPUqueue = new ArrayBlockingQueue<>(8);
        ArrayBlockingQueue<String> disk = new ArrayBlockingQueue<>(300); //Disk initialized with the constraint given by the assignment documentation

        //This chunk runs everything. J_SCHED runs first after the initial line is read in and sent to it.
        try {
            FileReader fileReader = new FileReader(file); //Preps input stream of file
            BufferedReader bufferedReader = new BufferedReader(fileReader); //Buffers input stream for main program loop

            while ((line = bufferedReader.readLine()) != null) {
                StringBuffer appendArrivalTime = new StringBuffer(line); //StringBuffer used to append 'job queue arrival time' to the incoming job string
                appendArrivalTime.append("	" + cpuTime);
                line = appendArrivalTime.toString(); //appends arrival time to incoming job

                if (sched.idCheck(line) == false) //checks if job id is 0, if so then fills ready queue from disk and calls J_DISPATCH to run.
                {                                //Returns false if no 0, true if there is a 0                      
                    if (disk.isEmpty() == false) {
                        if (disk.size() == 300) {
                            if (readyQueue.size() == qCONSTRAINT) //Empties readyQueue if its full coming into here
                            {
                                for (Iterator<String> it = readyQueue.iterator(); it.hasNext();) {
                                    J_DISPATCH(it.next());
                                    it.remove();
                                }
                            }
                            do 
                            {
                                if (appendLoadCheck(disk.element(),readyQueue))
                                {  
                                    disk.take();
                                }else //If no slot in memory open for job, it gets sent back to the disk to await its chance. In future builds, increase priority or summat.
				{
                                    disk.add(disk.element());
                                    disk.take();
                                }
                            } while (readyQueue.size() != qCONSTRAINT); 
                        }
						
                      //If the disk isn't full, but still has something on it, then run this operation. But check if the ready queue is full as well, to see if it needs to be ran.
                        if (readyQueue.size() != qCONSTRAINT) 
						{
                            int diskCount = 0;
                            do 
                            {	//If no jobs on the disk are suitable for load, break out of the loop and run jobs on ready queue as per 0 arrival. If jobs are suitable, load.
                                if (diskCount == disk.size()) 
				{
                                    break;
                                }
                                if (appendLoadCheck(disk.element(), readyQueue)) 
                                    disk.take();
                                else 
                                {
                                    disk.add(disk.element());
                                    disk.take();
                                    diskCount++;
                                }
                            } while (readyQueue.size() != qCONSTRAINT);
                        }
                        for (Iterator<String> it = readyQueue.iterator(); it.hasNext();) {
                            J_DISPATCH(it.next());
                            it.remove();
                        }
                    }
                } else //This is what goes on when incoming job id is NOT 0.
                {
                    if (sched.sizeCheck(line)) //If no 0, checks to see if it fits memory requirements. RejectedCount goes up if not.
                    {
                        rejectCount++;
                        continue;
                    } else {
                        if (disk.isEmpty() == false) //If the disk isn't empty, give it priority over incoming jobs to ready queue
                        {
                            if (disk.size() == 300) //Checks if the disk is full in order to avoid memory limitations.
							{
                                for (Iterator<String> it = readyQueue.iterator(); it.hasNext();) //If it is, immediately run any jobs and proceed to give
                                {
                                    J_DISPATCH(it.next());
                                    it.remove();
                                }

                                int diskCount = 0;
                                do //Loops through the disk until either ready queue is full or entire disk has been looked through.
                                {
                                    if (diskCount == disk.size()) {
                                        break;
                                    }

                                    if(appendLoadCheck(disk.element(), readyQueue))
                                        disk.take();
                                     else //If job on disk can't be added to ready queue, pop it off top of the stack and send it to the back.
                                    {
                                        disk.add(disk.element());
                                        disk.take();
                                        diskCount++;
                                    }
                                } while (readyQueue.size() != qCONSTRAINT);

                                for (Iterator<String> it = readyQueue.iterator(); it.hasNext();) {
                                    J_DISPATCH(it.next());
                                    it.remove();
                                }

                                disk.add(line);
                            }
							
			//Proceed with adding job to ready queue from disk since its not full
                            else
                            {
                                if (readyQueue.size() == qCONSTRAINT) //Empties readyQueue if its full coming into here
                                {
                                    for (Iterator<String> it = readyQueue.iterator(); it.hasNext();) {
                                        J_DISPATCH(it.next());
                                        it.remove();
                                    }
                                }
                            //All calls like this prep for load time appendation if job is found to be suitable for load.
                               StringBuffer appendLoadTimeD = new StringBuffer(disk.element()); 
                                String tempItemD = disk.element();
                                if (sched.aquireMemoryCheck(tempItemD)) {
                                    appendLoadTimeD.append("	" + cpuTime);
                                    tempItemD = appendLoadTimeD.toString();
                                    readyQueue.add(tempItemD);
                                    disk.take();
                                    disk.add(line);
                                } else {
                                    if (appendLoadCheck(line, readyQueue)) 
                                    {
                                       
                                    } else 
                                        disk.add(line);
                                }
                            }
                        } else //If the disk IS empty, come here to attempt to add incoming line to either ready queue or the disk.
                        {
                            if (readyQueue.size() == qCONSTRAINT) //Empties readyQueue if its full coming into here
                            {
                                for (Iterator<String> it = readyQueue.iterator(); it.hasNext();) {
                                    J_DISPATCH(it.next());
                                    it.remove();
                                }
                            }

                            if (sched.aquireMemoryCheck(line)) //Checks if new incoming job is good to load. If not, throws it on the disk.
							{
                                StringBuffer appendLoadTime = new StringBuffer(line);
                                String tempItem;
                                appendLoadTime.append("	" + cpuTime);
                                tempItem = appendLoadTime.toString();
                                readyQueue.add(tempItem);
                            } else {
                                disk.add(line);
                            }
                        }
                    }
                }
            }
            bufferedReader.close();
			
            while (disk.isEmpty() == false) //Empties disk after job input has ceased
            {
                String arrival = disk.take();
                String loaded;

                while (readyQueue.isEmpty() == false) {
                    J_DISPATCH(readyQueue.take());
                }
                if (sched.aquireMemoryCheck(arrival)) {
                    StringBuffer appendLoadTime = new StringBuffer(arrival);
                    appendLoadTime.append("	" + cpuTime);
                    loaded = appendLoadTime.toString();
                    readyQueue.add(loaded);
                } else {
                    disk.add(arrival);
                    disk.take();
                }
            }

            while (readyQueue.isEmpty() == false) //Empties ready queue after job input has ceased
            {
                J_DISPATCH(readyQueue.take());
            }

        } catch (FileNotFoundException ex) 
        {
            System.out.println("Unable to open file.");
        } catch (IOException ex) 
        {
            System.out.println("Error reading file.");
        } catch (Exception ex) 
        {
            ex.printStackTrace(System.out);
        }
        //Final output chunk of text
        System.out.println("\nNumber of jobs processed: " + jobsProcessed);
        System.out.println("Number of CPU-bound jobs: " + cpuJobCount);
        System.out.println("Number of Balanced jobs: " + balancedJobCount);
        System.out.println("Number of IO-bound jobs: " + ioJobCount);
        System.out.println("Average turnaround time: " + (totalTurnaround / jobsProcessed) + " milliseconds.");
        System.out.println("Average wait time: " + Math.abs(totalWaitTime / jobsProcessed) + " milliseconds.");
        System.out.println("Number of rejected jobs: " + rejectCount);
        System.out.println("Total processing time of CPU clock: " + cpuTime);
    }
}
