

/*
	This is the mem_manager subprogram. This subprogram checks whether or not any job currently being vetted by J_SCHED can actually be placed in memory, using the alloted memory granted to it by J_SCHED.
	Memory regions are abstracted from the documentation as a jagged array of booleans for ease of use. Memory regions are allocated by a series of if statements that runs a check on the incoming job's memory requirements as according to the project documentation. (EG - '4 regions of size 8k, 1 region of size 52k', etc.)
	A similar method is used when a job is terminating, and that same memory region has to be deallocated for future use.
	
	Possible Improvement: For simply checking which memory bit goes where, its a very long program. I do recall that a bitmap could be used in place of using if statements, but I was making fast and stable progress on the assignment with this method. In the future, I may revamp how the program checks memory so I can cut down on the size of this sub program.
        Changes made in Phase 2: Calculates and prints external and internal memory, as well as new function 'isFull', which checks to see if ALL memory is currently taken before trying to allocate a job to it.
*/
import java.io.*;
import java.util.*;

public class mem_manager
{
	public static boolean[][] workingArray; //This is the 'memory' for the system - a jagged 2D array of booleans created using the parameters outlined in the assignment documentation.
        public static double internalFragmentedMemory = 0; //Overall internal memory for all processed
        public static double externalFragmentedMemory = 0; //Overall external memory for the
	
	public mem_manager()
	{
	};
	
	public mem_manager(boolean[][] input) 
	{
		this.workingArray = input;
	};
	
	public static boolean aquire(int memoryNeed) //aquire is the method utilized when the scheduler is checking for memory availability
	{
		if(memoryNeed == 0) //memoryNeed is the memory requested by the job currently being scheduled
		{
			return false;
		}

		if((memoryNeed >= 1) && (memoryNeed <= 8))
		{
			for(int i = 0; i < workingArray[0].length; i++)
			{
				if(workingArray[0][i] == true)
				{
                                    internalFragmentedMemory += (8 - memoryNeed);
					workingArray[0][i] = false;
					return true;
				}
				else if(workingArray[0][i] == false)
					continue;
			}
		}
		else if((memoryNeed > 8) && (memoryNeed <= 12))
		{
			for(int i = 0; i < workingArray[1].length; i++)
			{
				if(workingArray[1][i] == true)
				{
                                    internalFragmentedMemory += (12 - memoryNeed);
					workingArray[1][i] = false;
					return true;
				}
				else
				{
					continue;
				}
			}
		}
		else if((memoryNeed > 12) && (memoryNeed <= 18))
		{
			for(int i = 0; i < workingArray[2].length; i++)
			{
				if(workingArray[2][i] == true)
				{
                                    internalFragmentedMemory += (18 - memoryNeed);
					workingArray[2][i] = false;
					return true;
				}
				else
					continue;
			}
		}
		else if((memoryNeed > 18) && (memoryNeed <= 32))
		{
			for(int i = 0; i < workingArray[3].length; i++)
			{
				if(workingArray[3][i] == true)
				{
                                    internalFragmentedMemory += (32 - memoryNeed);
					workingArray[3][i] = false;
					return true;
				}
				else
					continue;
			}
		}
		else if((memoryNeed > 32) && (memoryNeed <= 52))
		{
			for(int i = 0; i < workingArray[4].length; i++)
			{
				if(workingArray[4][i] == true)
				{
                                    internalFragmentedMemory += (52 - memoryNeed);
					workingArray[4][i] = false;
					return true;
				}
				else
					continue;
			}
		}
		else if((memoryNeed > 52) && (memoryNeed <= 60))
		{
			for(int i = 0; i < workingArray[5].length; i++)
			{
				if(workingArray[5][i] == true)
				{
                                        internalFragmentedMemory += 60 - memoryNeed;
					workingArray[5][i] = false;
					return true;
				}
				else
					continue;
			}
		}
		else if((memoryNeed > 60) && (memoryNeed <= 128))
		{
			for(int i = 0; i < workingArray[6].length; i++)
			{
				if(workingArray[6][i] == true)
				{
                                    internalFragmentedMemory += 128 - memoryNeed;
					workingArray[6][i] = false;
					return true;
				}
				else
					continue;
			}
		}
		
		return false;
	};
	
	public static boolean release(int memoryNeed) //release is the method called when the job has been terminated, and the memory its using has to be released for other incoming jobs.
	{
		if(memoryNeed <= 8)
		{
			for(int i = 0; i < workingArray[0].length; i++)
			{
				if(workingArray[0][i] == false)
				{
                                       externalFragmentedMemory += 8;
					workingArray[0][i] = true;
					return true;
				}
				else if(workingArray[0][i] == true)
					continue;
			}
		}
		else if((memoryNeed > 8) && (memoryNeed <= 12))
		{
			for(int i = 0; i < workingArray[1].length; i++)
			{
				if(workingArray[1][i] == false)
				{
                                        externalFragmentedMemory += 12;
					workingArray[1][i] = true;
					return true;
				}
				else
				{
					continue;
				}
			}
		}
		else if((memoryNeed > 12) && (memoryNeed <= 18))
		{
			for(int i = 0; i < workingArray[2].length; i++)
			{
				if(workingArray[2][i] == false)
				{
                                        externalFragmentedMemory += 18;
					workingArray[2][i] = true;
					return true;
				}
				else
					continue;
			}
		}
		else if((memoryNeed > 18) && (memoryNeed <= 32))
		{
			for(int i = 0; i < workingArray[3].length; i++)
			{
				if(workingArray[3][i] == false)
				{
                                        externalFragmentedMemory += 32;
					workingArray[3][i] = true;
					return true;
				}
				else
					continue;
			}
		}
		else if((memoryNeed > 32) && (memoryNeed <= 52))
		{
			for(int i = 0; i < workingArray[4].length; i++)
			{
				if(workingArray[4][i] == false)
				{
                                        externalFragmentedMemory += 52;
					workingArray[4][i] = true;
					return true;
				}
				else
					continue;
			}
		}
		else if((memoryNeed > 52) && (memoryNeed <= 60))
		{
			for(int i = 0; i < workingArray[5].length; i++)
			{
				if(workingArray[5][i] == false)
				{
                                        externalFragmentedMemory += 60;
					workingArray[5][i] = true;
					return true;
				}
				else
					continue;
			}
		}
		else if((memoryNeed > 60) && (memoryNeed <= 128))
		{
			for(int i = 0; i < workingArray[6].length; i++)
			{
				if(workingArray[6][i] == false)
				{
                                        externalFragmentedMemory += 128;
					workingArray[6][i] = true;
					return true;
				}
				else
					continue;
			}
		}
		return false;
	}
	public static boolean memoryAllowed(int memoryNeed) //This method checks if a job's memory requirements are larger than any of the currently available slots. If so, tells the scheduler to boot it out the system.
	{
		if(memoryNeed > 128)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
        
        public static boolean isFull()
        {
            int fullCount = 0;
            for(int i = 0; i < workingArray.length; i++)
            {
                for(int j = 0; j < workingArray[i].length; j++)
                {
                    if(workingArray[i][j] == false)
                    {
                        fullCount++;
                    }
                        
                }
            }
            
            if(fullCount == 26)
                return true;
            else
                return false;
        }
        
        public static void printExternal(double input)
        {
            System.out.printf("Average Internal Memory Fragmentation: %.2f KB\n", (internalFragmentedMemory/input));
            System.out.printf("Average External Memory Fragmentation: %.2f KB\n", (externalFragmentedMemory/input));
        }
	
	public static void main(String[] args)
	{
	}
}