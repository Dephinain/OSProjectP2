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
import java.util.HashMap;
import java.util.Map;
public class OSProjectPart2 {

    public static J_SCHED sched = new J_SCHED();
    public static int jobsProcessed;
    public static int cpuJobCount, ioJobCount, balancedJobCount, totalTurnaround, totalWaitTime, rejectCount, totalProcess, cpuTime;
    public static final int qCONSTRAINT = 26, qCONSTRAINT2 = 9;

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
            System.out.println("Traffic Count: " + term_tokens[8]);
            System.out.println("Priority at Termination: " + term_tokens[7]);
            System.out.println("Something: " + term_tokens[9]);
            sched.releaseMemory(finishedJob); //Inputs job string to release memory after execution
            totalTurnaround += tat_time;
            totalWaitTime += wait_time;
            jobsProcessed++;
        }

    }

    //Executes/processes incoming job from ready queue
    public static void J_DISPATCH(String arrivingJob) 
    {
       ArrayList<String> dispatch_tokens = new ArrayList<String>(); //ArrayList to hold to-be tokenized PCB
        dispatch_tokens = new ArrayList<String>(Arrays.asList(arrivingJob.split("\\s+"))); //Tokenizes executing job string
        HashMap<String, Integer> processTimer = new HashMap<>();
        int processedTime = 0;
        try 
        {
            /*
              if(dispatch_tokens.get(1).equals("1"))
              {
                    if(dispatch_tokens.get(7).equals("0"))
                    {
                    }
                    else
                        dispatch_tokens.set(7, Integer.toString(Integer.parseInt(dispatch_tokens.get(7)) - 1)); //Decrements the priority of the job
              } 
              else if(dispatch_tokens.get(1).equals("2"))
              {
                  if(dispatch_tokens.get(7).equals("1"))
                    {
                    }
                    else
                        dispatch_tokens.set(7, Integer.toString(Integer.parseInt(dispatch_tokens.get(7)) - 1));
              }
              else
                  dispatch_tokens.set(7, Integer.toString(Integer.parseInt(dispatch_tokens.get(7)) - 1));
              
              if(processTimer.containsKey(dispatch_tokens.get(1))) //Checks to see if job id exists already
              {
                 //If job id is found in hashmap processTimer, retrieve remaining processing time from processTimer and subtract relevant time quantum from it.
                 processedTime = processTimer.get(dispatch_tokens.get(1));
                 if(dispatch_tokens.get(2).equals("1"))
                     processedTime -= 75;
                 else if(dispatch_tokens.get(2).equals("2"))
                     processedTime -= 40;
                 else if(dispatch_tokens.get(2).equals("3"))
                     processedTime -= 20;
              }
              else
              {
                //If job id is not found in hashmap processTimer, retrieve processing time from PCB and subtract relevant time quantum from processing time.
                if(dispatch_tokens.get(2).equals("1"))
                      processedTime = (Integer.parseInt(dispatch_tokens.get(4)) - 75);
                else if(dispatch_tokens.get(2).equals("2"))
                      processedTime = (Integer.parseInt(dispatch_tokens.get(4)) - 40);
                else if(dispatch_tokens.get(2).equals("3"))
                      processedTime = (Integer.parseInt(dispatch_tokens.get(4)) - 20);
              }
            
              if(processedTime <= 0) //Checks to see if, after subtracting the time quantum, if the job is finished processing or not.
              {
                    //If job is finished processing, terminate it to remove it from memory and remove it from processTimer. Increment cpu clock by relevant time quantum.
                    //J_TERM(arrivingJob);
                    processTimer.remove(dispatch_tokens.get(1));
                    if(dispatch_tokens.get(2).equals("1"))
                        cpuTime += 75;
                    else if(dispatch_tokens.get(2).equals("2"))
                        cpuTime += 40;
                    else if(dispatch_tokens.get(2).equals("3"))
                        cpuTime += 20;
                    //return null;
              }
              else
              {
                  processTimer.put(dispatch_tokens.get(1), processedTime);
                  if(dispatch_tokens.get(2).equals("1"))
                      cpuTime += 75;
                  else if(dispatch_tokens.get(2).equals("2"))
                      cpuTime += 40;
                  else if(dispatch_tokens.get(2).equals("3"))
                      cpuTime += 20;
                  
                  if(dispatch_tokens.size() == 10)
                      dispatch_tokens.set(9, Integer.toString(cpuTime));       
                  //return String.join(" ", dispatch_tokens);
              }
           // */
            cpuTime += Integer.parseInt(dispatch_tokens.get(4));
            J_TERM(arrivingJob); //Calls after job 'execution' to output termination stats
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public static boolean appendLoadCheck(String incomingJob, Queue rQueue)
    {
        StringBuffer appendLoadTime = new StringBuffer(incomingJob);
        String[] tokens;
        tokens = incomingJob.split("\\s+");
        if(sched.aquireMemoryCheck(incomingJob))
        {
            appendLoadTime.append(" " + cpuTime); //Appends load time
            if(tokens[2].equals("1"))
                appendLoadTime.append(" " + "0"); //Appends Priority
            else if(tokens[2].equals("2"))
                appendLoadTime.append(" " + "1");
            else if(tokens[2].equals("3"))
                appendLoadTime.append(" " + "5");
            
            appendLoadTime.append(" " + "1"); //Appends traffic count
            appendLoadTime.append(" " + Integer.toString(cpuTime)); //Appends time for job priority time check
            incomingJob = appendLoadTime.toString();
            rQueue.add(incomingJob);
            return true;
        }
        else
            return false;
    }
    
    public static void timeCheck(Queue<String> IOqueue, Queue<String> otherQueue, int timeLimit)
    {
        for(Iterator<String> it = otherQueue.iterator(); it.hasNext();)
        {
           String temp = it.next();
           ArrayList<String> b_tokens = new ArrayList<String>();
           b_tokens = new ArrayList<String>(Arrays.asList(temp.split("\\s+")));
           if(b_tokens.size() == 10)
            {
               if(cpuTime - Integer.parseInt(b_tokens.get(9)) >= timeLimit)
               {
                  if(Integer.parseInt(b_tokens.get(7)) < 5)
                  {
                     b_tokens.set(7, Integer.toString(Integer.parseInt(b_tokens.get(7)) + 1));
                  }
                  else if(Integer.parseInt(b_tokens.get(7)) == 5)
                  {
                     if(IOqueue.size() < qCONSTRAINT2 && IOqueue.size() != qCONSTRAINT2)
                     {
                        //Increment the 'traffic count' since the job is switching queues.
                        IOqueue.add(temp);
                        it.remove();
                    }
                    else
                      continue; //Temporary measure until I figure out what breaks when this don't run
                  }
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        //File file = new File("/home/opsys/OS-I/18Sp-jobs");
        File file = new File("src/osprojectpart2/18Sp-jobs"); //Incoming job file
        String line; //Line that holds incoming job to be checked
        ArrayBlockingQueue<String> readyQueue = new ArrayBlockingQueue<>(qCONSTRAINT); //Ready queue initialized with the constraint
        Queue<String> IOqueue = new LinkedList<String>();
        Queue<String> balancedQueue = new LinkedList<String>();
        Queue<String> CPUqueue = new LinkedList<String>();
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
                    if (disk.isEmpty() == false) 
                    {
                        if (disk.size() == 300) 
                        {
                            if (readyQueue.size() == qCONSTRAINT) //Empties subqueues if its full coming into here. (Needs to empty all 3 subqueues if they're full)
                            {
                                //Current idea is to remove limits on queues and implement a 'full check' function that sees if all memory is currently taken. If so, no other jobs are allowed in until all jobs in queue are finished running
                                   /* if(sched.mem_full_check()) //Contingency to empty queue for filling if its full up
                                    {
                                        for(Iterator<String> it2 = IOqueue.iterator(); it2.hasNext();) //Potential issue: May cause exception for changing queue while iterating through it.
                                        {
                                            String input;
                                            input = J_DISPATCH(it2.next());
                                            it2.remove();
                                            if(input != null)
                                            {
                                                timeCheck(IOqueue, balancedQueue, 400);
                                                timeCheck(IOqueue, CPUqueue, 600);
                                
                                                ArrayList<String> tokens = new ArrayList<String>();
                                                tokens = new ArrayList(Arrays.asList(input.split("\\s+")));
                                                if(tokens.get(2).equals("1"))
                                                    CPUqueue.add(input);
                                                else if(tokens.get(2).equals("2"))
                                                    balancedQueue.add(input);
                                                else if(tokens.get(2).equals("3"))
                                                    IOqueue.add(input);
                                            }
                                            else if(input == null)
                                            {
                                                timeCheck(IOqueue, balancedQueue, 400);
                                                timeCheck(IOqueue, CPUqueue, 600);
                                            }       
                                         }  
                                     }
                                    
                                    do
                                    {
                                        ArrayList<String> tokens = new ArrayList<String>();
                                        tokens = new ArrayList(Arrays.asList(disk.element().split("\\s+")));
                                        if(tokens.get(2).equals("1"))
                                        {
                                            appendLoadCheck(disk.element(), CPUqueue);
                                            disk.remove();
                                        }
                                        else if(tokens.get(2).equals("2"))
                                        {
                                            appendLoadCheck(disk.element(), balancedQueue);
                                            disk.remove();
                                        }
                                        else if(tokens.get(2).equals("3"))
                                        {
                                            appendLoadCheck(disk.element(), IOqueue);
                                            disk.remove();
                                        }
                                    } while(sched.mem_full_check() != true);//*/
                                       
                                
                                for (Iterator<String> it = readyQueue.iterator(); it.hasNext();) 
                                {
                                    J_DISPATCH(it.next());
                                    it.remove();
                                }
                            }
                            
                            do 
                            {
                                if (appendLoadCheck(disk.element(),readyQueue)) //Need to check Job ID here for slotting into appropriate queue
                                    disk.take();
                                else //If no slot in memory open for job, it gets sent back to the disk to await its chance. In future builds, increase priority or summat.
				{
                                    disk.add(disk.element());
                                    disk.take();
                                }
                            } while (readyQueue.size() != qCONSTRAINT); 
                        }
						
                      //If the disk isn't full, but still has something on it, then run this operation. But check if the ready queue is full as well, to see if it needs to be ran.
                        if (readyQueue.size() != qCONSTRAINT) 
                        {
			/*if(sched.mem_full_check() != true) //For now, may not need the extra diskCount setup, but need to test
                        {
                            int diskCount = 0;
                            do
                            {
                                if(diskCount == disk.size())
                                {
                                    break;
                                }
                                ArrayList<String> tokens = new ArrayList<String>();
                                tokens = new ArrayList(Arrays.asList(disk.element().split("\\s+")));
                                if(tokens.get(2).equals("1"))
                                {
                                     appendLoadCheck(disk.element(), CPUqueue);
                                    disk.take();
                                }
                                else if(tokens.get(2).equals("2"))
                                {
                                    appendLoadCheck(disk.element(), balancedQueue);
                                    disk.take();
                                }
                                else if(tokens.get(2).equals("3"))
                                {
                                    appendLoadCheck(disk.element(), IOqueue);
                                    disk.take();
                                }
                            } while(sched.mem_full_check() != true);
                            
                            for(Iterator<String> it2 = IOqueue.iterator(); it2.hasNext();) //Potential issue: May cause exception for changing queue while iterating through it.
                            {                                                              //Another potential issue: Need to check if queues are EMPTY before running dispatch programs 
                               String input;
                               input = J_DISPATCH(it2.next());
                               it2.remove();
                               if(input != null)
                                {
                                  timeCheck(IOqueue, balancedQueue, 400);
                                  timeCheck(IOqueue, CPUqueue, 600);
                                
                                  ArrayList<String> tokens = new ArrayList<String>();
                                  tokens = new ArrayList(Arrays.asList(input.split("\\s+")));
                                  if(tokens.get(2).equals("1"))
                                       CPUqueue.add(input);
                                  else if(tokens.get(2).equals("2"))
                                        balancedQueue.add(input);
                                  else if(tokens.get(2).equals("3"))
                                        IOqueue.add(input);
                                }
                                else if(input == null)
                                {
                                    timeCheck(IOqueue, balancedQueue, 400);
                                    timeCheck(IOqueue, CPUqueue, 600);
                                }
                        }
                        //*/
                            int diskCount = 0;
                            do 
                            {	//If no jobs on the disk are suitable for load, break out of the loop and run jobs on ready queue as per 0 arrival. If jobs are suitable, load.
                                if (diskCount == disk.size()) 
				{
                                    break;
                                }
                                if (appendLoadCheck(disk.element(), readyQueue)) //Need to check job ID here
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

                                    if(appendLoadCheck(disk.element(), readyQueue)) //Need to check job ID here
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

                              if(appendLoadCheck(disk.element(), readyQueue))
                              {
                                  disk.take();
                                  disk.add(line);
                              }
                              else
                              {
                                if (appendLoadCheck(line, readyQueue)) //Need to check job ID here
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
                           
                           if(appendLoadCheck(line, readyQueue))
                           {
                               
                           }
                           else
                               disk.add(line);
                        }
                    }
                }
            }
            bufferedReader.close();
			
            while (disk.isEmpty() == false) //Empties disk after job input has ceased
            {
                String arrival = disk.take();
                while (readyQueue.isEmpty() == false) {
                    J_DISPATCH(readyQueue.take());
                }
                if(appendLoadCheck(arrival, readyQueue))
                {
                    
                }
                else
                {
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
        //System.out.println("Average internal fragmentation: ");
        //System.out.println("Total external fragmentation: ");
    }
}