// Copyright David Petrie 2008
//
// Connection between one worker and another.
import java.net.*;
import java.util.*;
import java.io.*;



public class WorkerConnection extends Thread
{
	private Socket neighbourChannel = null;
	private Worker worker = null;
	private PrintWriter out = null;
	private BufferedReader in = null;
	private boolean isReady = false;
	private boolean isDone = false;



	public WorkerConnection(Worker worker, Socket neighbour)
	{
		this.worker = worker;
		neighbourChannel = neighbour;
	}



	public void EstablishChannels() throws IOException
	{
		out = new PrintWriter(neighbourChannel.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(neighbourChannel.getInputStream()));
		isReady = true;

		String input = "";
		while(true) {
			input = in.readLine();
			isDone = false;
			if (input.equals("getBoundary")) {
				input = in.readLine();
				ArrayList<Cell> list = worker.GetBoundaryCells(input);

				for (Cell c : list) { 
					if (c == null) System.out.println("Null cell found!"); 
					else out.println(c.toCSV()); 
				}
				out.println("done");
				isDone = true;
			}
		}
	}



	public boolean isDone()
	{
		return isDone;		
	}



	public boolean isReady()
	{
		return isReady;
	}



	public void CloseChannels() throws IOException
	{
		out.close();
		in.close();
		neighbourChannel.close();		
	}



	public void run() {
		PrintWriter out = null;
		BufferedReader in = null;

		try {
			EstablishChannels();
		} catch (IOException e) {
			System.err.println(e);
		}
	}
}
