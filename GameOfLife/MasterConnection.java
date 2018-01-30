// Copyright David Petrie 2008
//
// Worker connection contains the socket used to connect between the
// master and each worker. The master communicates with connected
// workers through this class.
import java.net.*;
import java.util.*;
import java.io.*;



public class MasterConnection extends Thread
{
	private Socket workerChannel = null;
	private PrintWriter out = null;
	private BufferedReader in = null;
	private int id = 0;
	private int workerPort = 0;
	private String workerHostAddress = "";
	private boolean isReady = false;
	


	public MasterConnection(Socket w, int id)
	{
		workerChannel = w;
		this.id = id;
	}



	// Set up the channel between worker and master.
	public void EstablishChannels() throws IOException
	{
		out = new PrintWriter(workerChannel.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(workerChannel.getInputStream()));
		if (in.readLine().equals("worker")) {
			workerPort = Integer.parseInt(in.readLine());
			workerHostAddress = in.readLine();
		}
		isReady = true;
	}



	// Cnce worker is ready, sends cells allocated to the worker, 
	// and information about neighbouring workers.
	public void SendWorkInformation(Cell[][] cells) throws IOException
	{
		String input = "";

		while(( input = in.readLine()) != null) {
			if (input.equals("neighbours")) {
				while(Master.GetPossibleNeighbours(this) < 8);
				for (Direction d: Direction.values()) {
					MasterConnection n = Master.GetNeighbour(this, d);
					if (n == null) out.println("NULL");
					else out.println(d + ", " + n.GetAddress());
				}
				out.println("done");
			} else if (input.equals("cells")) {
				GameOfLifeUtils.PrintCells(cells, out);
				out.println("done");
				break;
			} else if (input.equals("rows")) {
				out.println(cells.length);
			} else if (input.equals("cols")) {
				out.println(cells[0].length);
			}
		}
	}



	// Once the worker is ready send it a "next state" heart beat.
	public void SetNextState() throws IOException
	{
		String input = in.readLine();
		while (!input.equals("ready")) input = in.readLine();
		out.println("nextState");
	}



	// Gets cells from the worker.
	public ArrayList<Cell> GetCells() throws IOException
	{
		ArrayList<Cell> list = new ArrayList();
		String input = in.readLine();
		if (input.equals("cells")) {
			while(true) {
				input = in.readLine();
				if (input.equals("done")) break;
				Cell tmp = new Cell(input);
				list.add(tmp);
			}
		}
		return list;
	}



	public void CloseChannels() throws IOException
	{
		out.println("GameOver");
		out.close();
		in.close();
		workerChannel.close();		
	}



	public String GetInfo()
	{
		return "Worker: " + this.id + ", " + workerChannel.getPort();
	}



	public boolean isReady()
	{
		return isReady;
	}



	public String GetAddress()
	{
		return workerHostAddress + ", " + workerPort;
	}



	public int GetID()
	{
		return this.id;
	}



	public void run() {
		try {
			EstablishChannels();
		} catch (IOException e) {
			System.err.println(e);
		}
	}
}


