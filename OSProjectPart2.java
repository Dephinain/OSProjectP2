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

public class OSProjectPart2
{

    public static J_SCHED sched = new J_SCHED();
    public static int jobsProcessed;
    public static int cpuJobCount, ioJobCount, balancedJobCount, totalTurnaround, totalWaitTime, rejectCount, totalProcess, cpuTime, missingBalance;
    public static final int qCONSTRAINT = 26, qCONSTRAINT2 = 9;
    public static HashMap<String, Integer> processTimer = new HashMap<>();

    //Job termination function. Documents and outputs job termination statistics, and purges the appropriate queues upon termination.
    public static void J_TERM(String finishedJob)
    {
        //System.out.println("J_TERM is being called.");
        long term_time = cpuTime; //Termination time
        long tat_time, wait_time; //Turnaround time and wait time
        String[] term_tokens; //Array to hold to-be tokenized job string
        term_tokens = finishedJob.split("\\s+"); //Tokenizes terminating job string
        if (Integer.parseInt(term_tokens[1]) == 0)
        {
        } else
        {
            tat_time = term_time - Math.abs(Long.parseLong(term_tokens[4]) - Long.parseLong(term_tokens[5]));
            wait_time = term_time - Long.parseLong(term_tokens[5]);
            switch (Integer.parseInt(term_tokens[2]))
            {
                case 1:
                    cpuJobCount++;
                    break;
                case 2:
                    balancedJobCount++;
                    break;
                case 3:
                    ioJobCount++;
                    break;
                default:
                    break;
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
            System.out.println("Priority at Termination: " + term_tokens[7]);
            System.out.println("Traffic count: " + term_tokens[8]);
             System.out.println("Cpu time at termination: " + term_tokens[9]);
            sched.releaseMemory(finishedJob); //Inputs job string to release memory after execution
            totalTurnaround += tat_time;
            totalWaitTime += wait_time;
            jobsProcessed++;
        }

    }

    //Executes/processes incoming job from ready queue
    public static String J_DISPATCH(String arrivingJob)
    {
        ArrayList<String> dispatch_tokens = new ArrayList<>(); //ArrayList to hold to-be tokenized PCB
        dispatch_tokens = new ArrayList<>(Arrays.asList(arrivingJob.split("\\s+"))); //Tokenizes executing job string
        int processedTime = 0, timeIntermed;
        try
        {
            switch (dispatch_tokens.get(2)) //Checks job id so that its priority never goes below 1 for balanced jobs and 0 for CPU jobs
            {
                case "1":
                    if (dispatch_tokens.get(7).equals("0"))
                    {
                    } else
                    {
                        dispatch_tokens.set(7, Integer.toString(Integer.parseInt(dispatch_tokens.get(7)) - 1)); //Decrements the priority of the job
                    }
                    break;
                case "2":
                    if (dispatch_tokens.get(7).equals("1"))
                    {
                    } else
                    {
                        dispatch_tokens.set(7, Integer.toString(Integer.parseInt(dispatch_tokens.get(7)) - 1));
                    }
                    break;
                default:
                    dispatch_tokens.set(7, Integer.toString(Integer.parseInt(dispatch_tokens.get(7)) - 1));
                    break;
            }

            if (processTimer.containsKey(dispatch_tokens.get(1))) //Checks to see if job id exists already
            {
                //If job id is found in hashmap processTimer, retrieve remaining processing time from processTimer and subtract relevant time quantum from it.
                processedTime = processTimer.get(dispatch_tokens.get(1));
                switch (dispatch_tokens.get(2))
                {
                    case "1":
                        processedTime -= 75;
                        break;
                    case "2":
                        processedTime -= 40;
                        break;
                    case "3":
                        processedTime -= 20;
                        break;
                    default:
                        break;
                }
            } else
            {
                //If job id is not found in hashmap processTimer, retrieve processing time from PCB and subtract relevant time quantum from processing time.
                switch (dispatch_tokens.get(2))
                {
                    case "1":
                        processedTime = (Integer.parseInt(dispatch_tokens.get(4)) - 75);
                        break;
                    case "2":
                        processedTime = (Integer.parseInt(dispatch_tokens.get(4)) - 40);
                        break;
                    case "3":
                        processedTime = (Integer.parseInt(dispatch_tokens.get(4)) - 20);
                        break;
                    default:
                        break;
                }
            }

            if (processedTime <= 0) //Checks to see if, after subtracting the time quantum, if the job is finished processing or not.
            {
                //System.out.println("Time is less than 0!");
                //If job is finished processing, terminate it to remove it from memory and remove it from processTimer. Increment cpu clock by relevant time quantum.
               // if(dispatch_tokens.get(2).equals("2"))
                  //  missingBalance++;
               // System.out.println("Missing balance is: " + missingBalance);
                J_TERM(arrivingJob);
                switch (dispatch_tokens.get(2))
                {
                    case "1":
                        if(processTimer.containsKey(dispatch_tokens.get(1)))
                            cpuTime += (processedTime - processTimer.get(dispatch_tokens.get(1))); //Add remaining time from processed time instead of just flat time from overall processing time
                        else
                            cpuTime += Integer.parseInt(dispatch_tokens.get(4));
                        break;
                    case "2":
                        cpuTime += Integer.parseInt(dispatch_tokens.get(4));
                        break;
                    case "3":
                        cpuTime += Integer.parseInt(dispatch_tokens.get(4));
                        break;
                    default:
                        break;
                }
                if(processTimer.containsKey(dispatch_tokens.get(1)))
                    processTimer.remove(dispatch_tokens.get(1));
                return null;
            } else
            {
                //System.out.println("Time is greater than 0! Being sent back to queue!");
                processTimer.put(dispatch_tokens.get(1), processedTime);
                switch (dispatch_tokens.get(2))
                {
                    case "1":
                        cpuTime += processedTime;
                        break;
                    case "2":
                        cpuTime += processedTime;
                        break;
                    case "3":
                        cpuTime += processedTime;
                        break;
                    default:
                        break;
                }

                if (dispatch_tokens.size() == 10)
                {
                    dispatch_tokens.set(9, Integer.toString(cpuTime));
                }
                return String.join(" ", dispatch_tokens);
            }
            // */
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
    }

    public static boolean appendLoadCheck(String incomingJob, Queue rQueue)
    {
        StringBuffer appendLoadTime = new StringBuffer(incomingJob);
        String[] tokens;
        tokens = incomingJob.split("\\s+");
        if (sched.aquireMemoryCheck(incomingJob))
        {
            appendLoadTime.append(" " + cpuTime); //Appends load time
            switch (tokens[2])
            {
                case "1":
                    appendLoadTime.append(" " + "0"); //Appends Priority
                    break;
                case "2":
                    appendLoadTime.append(" " + "1");
                    break;
                case "3":
                    appendLoadTime.append(" " + "5");
                    break;
                default:
                    break;
            }

            appendLoadTime.append(" " + "1"); //Appends traffic count
            appendLoadTime.append(" ").append(Integer.toString(cpuTime)); //Appends time for job priority time check
            incomingJob = appendLoadTime.toString();
            rQueue.add(incomingJob);
            return true;
        } else
        {
            return false;
        }
    }

    public static void timeCheck(Queue<String> IOqueue, Queue<String> otherQueue, int timeLimit)
    {
        for (Iterator<String> it = otherQueue.iterator(); it.hasNext();)
        {
            String temp = it.next();
            ArrayList<String> b_tokens = new ArrayList<>();
            b_tokens = new ArrayList<>(Arrays.asList(temp.split("\\s+")));
            if (b_tokens.size() == 10)
            {
                if((cpuTime - Integer.parseInt(b_tokens.get(9))) >= timeLimit)
                {
                    b_tokens.set(7, Integer.toString(Integer.parseInt(b_tokens.get(7)) + 1));

                    if (Integer.parseInt(b_tokens.get(7)) == 5)
                    {
                        System.out.println("Priority high enough to get put into IO queue!");
                        IOqueue.add(temp);
                        it.remove();
                    }
                }
            }
        }
    }

    public static void memoryDump(Queue<String> IOqueue, Queue<String> balancedQueue, Queue<String> CPUqueue) //Used when clearing memory is required
    {
        String tempInput;
        while (IOqueue.isEmpty() == false)
        {
            //System.out.println("Infinite loop!");
            tempInput = J_DISPATCH(IOqueue.element());
            IOqueue.remove();
            if (tempInput != null)
            {
                timeCheck(IOqueue, balancedQueue, 400);
                timeCheck(IOqueue, CPUqueue, 600);

                ArrayList<String> tokens = new ArrayList<>();
                tokens = new ArrayList(Arrays.asList(tempInput.split("\\s+")));
                switch (tokens.get(2))
                {
                    case "1":
                        CPUqueue.add(tempInput);
                        break;
                    case "2":
                        balancedQueue.add(tempInput);
                        break;
                    case "3":
                        IOqueue.add(tempInput);
                        break;
                    default:
                        break;
                }
            } else
            {
                timeCheck(IOqueue, balancedQueue, 400);
                timeCheck(IOqueue, CPUqueue, 600);
            }
        }

        while (balancedQueue.isEmpty() == false)
        {
            tempInput = J_DISPATCH(balancedQueue.element());
            balancedQueue.remove();
            if (tempInput != null)
            {
                timeCheck(IOqueue, balancedQueue, 400);
                timeCheck(IOqueue, CPUqueue, 600);

                ArrayList<String> tokens = new ArrayList<>();
                tokens = new ArrayList(Arrays.asList(tempInput.split("\\s+")));
                switch (tokens.get(2))
                {
                    case "1":
                        CPUqueue.add(tempInput);
                        break;
                    case "2":
                        balancedQueue.add(tempInput);
                        break;
                    case "3":
                        IOqueue.add(tempInput);
                        break;
                    default:
                        break;
                }
            } else
            {
                timeCheck(IOqueue, balancedQueue, 400);
                timeCheck(IOqueue, CPUqueue, 600);
            }
        }

        while (CPUqueue.isEmpty() == false)
        {
            //System.out.println("Infinite loop!");
            tempInput = J_DISPATCH(CPUqueue.element());
            CPUqueue.remove();
            if (tempInput != null)
            {
                timeCheck(IOqueue, balancedQueue, 400);
                timeCheck(IOqueue, CPUqueue, 600);

                ArrayList<String> tokens = new ArrayList<>();
                tokens = new ArrayList(Arrays.asList(tempInput.split("\\s+")));
                switch (tokens.get(2))
                {
                    case "1":
                        CPUqueue.add(tempInput);
                        break;
                    case "2":
                        balancedQueue.add(tempInput);
                        break;
                    case "3":
                        IOqueue.add(tempInput);
                        break;
                    default:
                        break;
                }
            } else
            {
                timeCheck(IOqueue, balancedQueue, 400);
                timeCheck(IOqueue, CPUqueue, 600);
            }
        }

    }
    
    public static void jobRun(Queue<String> IOqueue, Queue<String> balancedQueue, Queue<String> CPUqueue)
    {
        String tempInput;
        if(IOqueue.isEmpty() == false)
        {
            tempInput = J_DISPATCH(IOqueue.element());
            IOqueue.remove();
            if (tempInput != null)
            {
                timeCheck(IOqueue, balancedQueue, 400);
                timeCheck(IOqueue, CPUqueue, 600);

                ArrayList<String> tokens = new ArrayList<>();
                tokens = new ArrayList(Arrays.asList(tempInput.split("\\s+")));
                switch (tokens.get(2))
                {
                    case "1":
                        CPUqueue.add(tempInput);
                        break;
                    case "2":
                        balancedQueue.add(tempInput);
                        break;
                    case "3":
                        IOqueue.add(tempInput);
                        break;
                    default:
                        break;
                }
            }
        }
        else if(balancedQueue.isEmpty() == false)
        {
            tempInput = J_DISPATCH(balancedQueue.element());
            balancedQueue.remove();
            if (tempInput != null)
            {
                timeCheck(IOqueue, balancedQueue, 400);
                timeCheck(IOqueue, CPUqueue, 600);

                ArrayList<String> tokens = new ArrayList<>();
                tokens = new ArrayList(Arrays.asList(tempInput.split("\\s+")));
                switch (tokens.get(2))
                {
                    case "1":
                        CPUqueue.add(tempInput);
                        break;
                    case "2":
                        balancedQueue.add(tempInput);
                        break;
                    case "3":
                        IOqueue.add(tempInput);
                        break;
                    default:
                        break;
                }
            }
        }
        else if(CPUqueue.isEmpty() == false)
        {
            tempInput = J_DISPATCH(CPUqueue.element());
            CPUqueue.remove();
            if (tempInput != null)
            {
                timeCheck(IOqueue, balancedQueue, 400);
                timeCheck(IOqueue, CPUqueue, 600);

                ArrayList<String> tokens = new ArrayList<>();
                tokens = new ArrayList(Arrays.asList(tempInput.split("\\s+")));
                switch (tokens.get(2))
                {
                    case "1":
                        CPUqueue.add(tempInput);
                        break;
                    case "2":
                        balancedQueue.add(tempInput);
                        break;
                    case "3":
                        IOqueue.add(tempInput);
                        break;
                    default:
                        break;
                }
            }
        }
    }


    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws InterruptedException, IOException
    {
        //File file = new File("/home/opsys/OS-I/18Sp-jobs");
        File file = new File("src/osprojectpart2/18Sp-jobs"); //Incoming job file
        String line; //Line that holds incoming job to be checked
        Queue<String> IOqueue = new LinkedList<>();
        Queue<String> balancedQueue = new LinkedList<>();
        Queue<String> CPUqueue = new LinkedList<>();
        ArrayBlockingQueue<String> disk = new ArrayBlockingQueue<>(300); //Disk initialized with the constraint given by the assignment documentation

        //This chunk runs everything. J_SCHED runs first after the initial line is read in and sent to it.
        try
        {
            FileReader fileReader = new FileReader(file); //Preps input stream of file
            BufferedReader bufferedReader = new BufferedReader(fileReader); //Buffers input stream for main program loop

            while ((line = bufferedReader.readLine()) != null)
            {
                StringBuffer appendArrivalTime = new StringBuffer(line); //StringBuffer used to append 'job queue arrival time' to the incoming job string
                appendArrivalTime.append("	").append(cpuTime);
                line = appendArrivalTime.toString(); //appends arrival time to incoming job

                if (sched.idCheck(line) == false) //checks if job id is 0, if so then fills ready queue from disk and calls J_DISPATCH to run.
                {                                //Returns false if no 0, true if there is a 0                      
                    if (disk.isEmpty() == false)
                    {
                        if (disk.size() == 300)
                        {
                            if (sched.mem_full_check()) //Empty all memory so disk can unload and not die
                            {
                                memoryDump(IOqueue, balancedQueue, CPUqueue);
                            }
                            int diskCount = 0;
                            do
                            {
                                System.out.println("We's loopin again, boss"); //Infinite loop hitting here
                                //int diskCount = 0;
                                if (diskCount == disk.size())
                                {
                                    break;
                                }
                                ArrayList<String> tokens = new ArrayList<String>();
                                String temp;
                                tokens = new ArrayList(Arrays.asList(disk.element().split("\\s+")));
                                switch (tokens.get(2))
                                {
                                    case "1":
                                        if (appendLoadCheck(disk.element(), CPUqueue))
                                        {
                                            disk.take();
                                        } else
                                        {
                                            temp = disk.element();
                                            disk.take();
                                            disk.add(temp);
                                            diskCount++;
                                        }
                                        break;
                                    case "2":
                                        if (appendLoadCheck(disk.element(), balancedQueue))
                                        {
                                            disk.take();
                                        } else
                                        {
                                            temp = disk.element();
                                            disk.take();
                                            disk.add(temp);
                                            diskCount++;
                                        }
                                        break;
                                    case "3":
                                        if (appendLoadCheck(disk.element(), IOqueue))
                                        {
                                            disk.take();
                                        } else
                                        {
                                            temp = disk.element();
                                            disk.take();
                                            disk.add(temp);
                                            diskCount++;
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            } while (sched.mem_full_check() != true);
                        }
                    }
                }
                {
                    if (sched.sizeCheck(line)) //If no 0, checks to see if it fits memory requirements. RejectedCount goes up if not.
                    {
                        rejectCount++;
                        continue;
                    } else
                    {
                        if (disk.isEmpty() == false) //If the disk isn't empty, give it priority over incoming jobs to ready queue
                        {
                            if (disk.size() == 300) //Checks if the disk is full in order to avoid memory limitations.
                            {
                                if (sched.mem_full_check()) //Contingency to empty queue for filling if its full up
                                {
                                    memoryDump(IOqueue, balancedQueue, CPUqueue);
                                }

                                if (sched.mem_full_check() != true)
                                {

                                    int diskCount = 0;
                                    String temp;
                                    do
                                    {
                                        //System.out.println("Caught in an infinite loop!");
                                        if (diskCount == disk.size())
                                        {
                                            break;
                                        }
                                        ArrayList<String> tokens = new ArrayList<String>();
                                        tokens = new ArrayList(Arrays.asList(disk.element().split("\\s+")));
                                        switch (tokens.get(2))
                                        {
                                            case "1":
                                                if (appendLoadCheck(disk.element(), CPUqueue))
                                                {
                                                    disk.take();
                                                } else
                                                {
                                                    temp = disk.element();
                                                    disk.take();
                                                    disk.add(temp);
                                                    diskCount++;
                                                }
                                                break;
                                            case "2":
                                                if (appendLoadCheck(disk.element(), balancedQueue))
                                                {
                                                    disk.take();
                                                } else
                                                {
                                                    temp = disk.element();
                                                    disk.take();
                                                    disk.add(temp);
                                                    diskCount++;
                                                }
                                                break;
                                            case "3":
                                                if (appendLoadCheck(disk.element(), IOqueue))
                                                {
                                                    disk.take();
                                                } else
                                                {
                                                    temp = disk.element();
                                                    disk.take();
                                                    disk.add(temp);
                                                    diskCount++;
                                                }
                                                break;
                                            default:
                                                break;
                                        }
                                    } while (sched.mem_full_check() != true);
                                }
                                memoryDump(IOqueue, balancedQueue, CPUqueue);
                                ArrayList<String> tokens = new ArrayList<String>();
                                tokens = new ArrayList(Arrays.asList(line.split("\\s+")));
                                switch (tokens.get(2))
                                {
                                    case "1":
                                        if (appendLoadCheck(line, CPUqueue))
                                        {

                                        } else
                                        {
                                            disk.add(line);
                                        }
                                        break;
                                    case "2":
                                        if (appendLoadCheck(line, balancedQueue))
                                        {

                                        } else
                                        {
                                            disk.add(line);
                                        }
                                        break;
                                    case "3":
                                        if (appendLoadCheck(line, IOqueue))
                                        {

                                        } else
                                        {
                                            disk.add(line);
                                        }
                                        break;
                                    default:
                                        break;
                                }

                                //disk.add(line);
                            } //Proceed with adding job to ready queue from disk since its not full
                            else
                            {
                                if (sched.mem_full_check()) //Checks to see if memory is full.
                                {
                                    memoryDump(IOqueue, balancedQueue, CPUqueue);
                                } else
                                {
                                    ArrayList<String> tokens = new ArrayList<String>();
                                    tokens = new ArrayList(Arrays.asList(disk.element().split("\\s+")));
                                    switch (tokens.get(2))
                                    {
                                        case "1":
                                            if (appendLoadCheck(disk.element(), CPUqueue))
                                            {
                                                disk.take();
                                                disk.add(line);
                                            } else if (appendLoadCheck(disk.element(), CPUqueue) == false)
                                            {
                                                tokens = new ArrayList(Arrays.asList(line.split("\\s+")));
                                                switch (tokens.get(2))
                                                {
                                                    case "1":
                                                        if (appendLoadCheck(line, CPUqueue) == false)
                                                        {
                                                            disk.add(line);
                                                        }
                                                        break;
                                                    case "2":
                                                        if (appendLoadCheck(line, balancedQueue) == false)
                                                        {
                                                            disk.add(line);
                                                        }
                                                        break;
                                                    case "3":
                                                        if (appendLoadCheck(line, IOqueue) == false)
                                                        {
                                                            disk.add(line);
                                                        }
                                                        break;
                                                }
                                            } else
                                            {
                                                disk.add(line);
                                            }
                                            break;
                                        case "2":
                                            if (appendLoadCheck(disk.element(), balancedQueue))
                                            {
                                                disk.take();
                                                disk.add(line);
                                            } else if (appendLoadCheck(disk.element(), balancedQueue) == false)
                                            {
                                                tokens = new ArrayList(Arrays.asList(line.split("\\s+")));
                                                switch (tokens.get(2))
                                                {
                                                    case "1":
                                                        if (appendLoadCheck(line, CPUqueue) == false)
                                                        {
                                                            disk.add(line);
                                                        }
                                                        break;
                                                    case "2":
                                                        if (appendLoadCheck(line, balancedQueue) == false)
                                                        {
                                                            disk.add(line);
                                                        }
                                                        break;
                                                    case "3":
                                                        if (appendLoadCheck(line, IOqueue) == false)
                                                        {
                                                            disk.add(line);
                                                        }
                                                        break;
                                                }
                                            } else
                                            {
                                                disk.add(line);
                                            }
                                            break;
                                        case "3":
                                            if (appendLoadCheck(disk.element(), IOqueue))
                                            {
                                                disk.take();
                                                disk.add(line);
                                            } else if (appendLoadCheck(disk.element(), IOqueue) == false)
                                            {
                                                tokens = new ArrayList(Arrays.asList(line.split("\\s+")));
                                                switch (tokens.get(2))
                                                {
                                                    case "1":
                                                        if (appendLoadCheck(line, CPUqueue) == false)
                                                        {
                                                            disk.add(line);
                                                        }
                                                        break;
                                                    case "2":
                                                        if (appendLoadCheck(line, balancedQueue) == false)
                                                        {
                                                            disk.add(line);
                                                        }
                                                        break;
                                                    case "3":
                                                        if (appendLoadCheck(line, IOqueue) == false)
                                                        {
                                                            disk.add(line);
                                                        }
                                                        break;
                                                }
                                            } else
                                            {
                                                disk.add(line);
                                            }
                                            break;
                                    }
                                }
                            }
                        } else //If the disk IS empty, come here to attempt to add incoming line to either ready queue or the disk.
                        {
                            ArrayList<String> tokens = new ArrayList<String>();
                            tokens = new ArrayList(Arrays.asList(line.split("\\s+")));
                            switch (tokens.get(2))
                            {
                                case "1":
                                    if (appendLoadCheck(line, CPUqueue))
                                    {

                                    } else
                                    {
                                        disk.add(line);
                                    }
                                    break;
                                case "2":
                                    if (appendLoadCheck(line, balancedQueue))
                                    {

                                    } else
                                    {
                                        disk.add(line);
                                    }
                                    break;
                                case "3":
                                    if (appendLoadCheck(line, IOqueue))
                                    {

                                    } else
                                    {
                                        disk.add(line);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
            bufferedReader.close();

            while (disk.isEmpty() == false) //Empties disk after job input has ceased
            {
                if (sched.mem_full_check())
                {
                    memoryDump(IOqueue, balancedQueue, CPUqueue);
                } else
                {
                    ArrayList<String> tokens = new ArrayList<String>();
                    String temp;
                    tokens = new ArrayList(Arrays.asList(disk.element().split("\\s+")));
                    switch (tokens.get(2))
                    {
                        case "1":
                            if (appendLoadCheck(disk.element(), CPUqueue))
                            {
                                disk.take();
                            } else
                            {
                                temp = disk.element();
                                disk.take();
                                disk.add(temp);
                            }
                            break;
                        case "2":
                            if (appendLoadCheck(disk.element(), balancedQueue))
                            {
                                disk.take();
                            } else
                            {
                                temp = disk.element();
                                disk.take();
                                disk.add(temp);
                            }
                            break;
                        case "3":
                            if (appendLoadCheck(disk.element(), IOqueue))
                            {
                                disk.take();
                            } else
                            {
                                temp = disk.element();
                                disk.take();
                                disk.add(temp);
                            }
                            break;
                        default:
                            break;
                    }
                    memoryDump(IOqueue, balancedQueue, CPUqueue);
                }
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
