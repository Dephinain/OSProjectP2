/*
File Description: This is the Job Scheduler proper, where all decisions regarding whether or not it's loaded to the ready queue, the disk, or not loaded at all take place.

Job mix explanation: During my testing of this program, I opted to go for a 'process everything as it comes in' approach after previous attempts at a IO -> balanced -> CPU job(highest priority at left)priority structure ended up with the program spending a not-insignificant amount of time simply trying to queue up IO and balanced marked jobs. For a smaller job list, my implementation may have worked, but considering the sheer amount of CPU jobs vs
the other types of jobs, I felt it prudent to just process everything as it came in, which lessened run time considerably.

Possible Imrpovement: The idCheck function doesn't properly work correctly when it comes to actually keeping 0 - or empty - jobs off the actual readyQueue and disk. Could be better implemented, but it does its job
in actually checking that a 0/empty job has come in, and to run the protocols outlined in the project documentation.
*/

import java.io.*;
import java.util.*;

public class J_SCHED
{
		private void initialize(boolean[][] input) //This function initializes the memory block array to be true - as in, all memory regions are currently open at the start of input.
		{
			for(int i = 0; i < input.length; i++)
			{
				for(int j = 0; j < input[i].length; j++)
				{
					input[i][j] = true;
				}
			}
		}
		
		private static boolean[][] memoryArr = new boolean[7][]; //The designated region of memory
		
		public J_SCHED() //Creating the J_SCHED object initializes the memory region to be open to incoming jobs
		{			
			memoryArr[0] = new boolean[4];
			memoryArr[1] = new boolean[4];
			memoryArr[2] = new boolean[6];
			memoryArr[3] = new boolean[6];
			memoryArr[4] = new boolean[1];
			memoryArr[5] = new boolean[4];
			memoryArr[6] = new boolean[1];
			initialize(memoryArr);
		};
		//Note: tokens[1-6] represent the following: Job ID(1), class of Job(2), requested memory(3), processing time(4), arrival time(5), time job was loaded to ready queue(6).
		private static mem_manager manager = new mem_manager(memoryArr); //Creates an instance of mem_manager for properly allocating and deallocating memory
		
		public static boolean aquireMemoryCheck(String inputLine) //Checks with mem_manager to see if any memory available. If so, replies to main program to insert it into the queue.
		{
			String[] tokens; //Array to hold to-be tokenized incoming job string
			tokens = inputLine.split("\\s+"); //Tokenized job string
			if(manager.aquire(Integer.parseInt(tokens[3])))
			{
				return true;
			}
			else
				return false;
		};
		
		public static boolean releaseMemory(String inputLine) //Checks with mem_manager to properly release any memory currently utilized by the system by the terminated job.
		{
			String[] tokens;
			tokens = inputLine.split("\\s+");	
			if(manager.release(Integer.parseInt(tokens[3])))
			{
				return true;
			}
			else
				return false;
		};
		
		public static boolean idCheck(String inputLine) //Checks the ID of the incoming job to see if its an actual job or an empty line
		{
			String[] tokens;
			tokens = inputLine.split("\\s+");
			if(Integer.parseInt(tokens[1]) != 0)
			{
				return true;
			}
			else
				return false;
		}
		
		public static boolean sizeCheck(String input) //Checks with mem_manager to see if the size of the job will fit within the alloted memory regions.
		{
			String[] check_tokens;
			check_tokens = input.split("\\s+");
			if((manager.memoryAllowed(Integer.parseInt(check_tokens[3]))) == true)
				return true;
			else
				return false;
		}
		public static void main(String[] args)
		{
		}
}