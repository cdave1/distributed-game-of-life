// Copyright David Petrie 2008
//
// Worker process.
//
// Attempts to connect to a master at an address and port. 
// Once it does this, it downloads a cell set and begins cycling
// through stages of the game. 
//
// Spawns a WorkerConnectionHandler for handling incoming
// connections from other worker processes. This connection 
// handler also runs in a separate thread.
import java.net.*;
import java.io.*;
import java.util.*;



public class Worker
{
	private ServerSocket dataServer = null;
	private Hashtable<String, Socket> neighbours = null;
	private WorkerConnectionHandler neighbourConnections = null;
	private Socket masterConnection = null;
	private Cell[][] previousCells = null;
	private Cell[][] currentCells = null;
	private PrintWriter out = null;
	private BufferedReader in = null;
	private int workerPort = 0;
	private int masterPort = 0;
	private String masterHost = "localhost";



	public Worker(int workerPort, String masterHost, int masterPort) {
		this.workerPort = workerPort;
		this.masterHost = masterHost;
		this.masterPort = masterPort;
		try {
			dataServer = new ServerSocket(workerPort);
			neighbourConnections = new WorkerConnectionHandler(this, dataServer);
			neighbourConnections.start();
		} catch (IOException e) {
			System.exit(-1);
		}
	}



	// Waits until connection to master can be established.
	//
	// Gets a list of neighbouring workers (and their ports) from the master.
	// Set up new connection threads
	public void EstablishConnections() throws IOException
	{
		try {
			masterConnection = new Socket(masterHost, masterPort);
			System.out.println("Worker successfully connected to master!");
			out = new PrintWriter(masterConnection.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(masterConnection.getInputStream()));
			out.println("worker");
			out.println(this.workerPort);
			out.println(((InetSocketAddress)dataServer.getLocalSocketAddress()).getHostName());
		} catch (ConnectException e) {
			// wait and try again
			System.out.println("Connection refused. Waiting 5 seconds before trying again...");
			try { Thread.sleep(5000); } 
			catch (InterruptedException ex) { System.exit(-1); }
			EstablishConnections();
		} catch (Exception e) {
			System.err.println(e);
			System.exit(-1);
		}	
	}



	// Request neighbours and cell information from master.
	//
	// Method is synchronized for two reasons:
	// - Need to wait until all cells are received from master, so
	//   we can prevent requesting neihbours receiving null cells.
	// - Also want to wait until all neighbours are connected, making
	//   sure nothing touches any of the neighbours until they've all
	//   connected (this will block any neighbours trying to get cells).
	private synchronized void GetSettings() throws IOException
	{
		String input;
		neighbours = new Hashtable();

		GetNeighbours();
		
		out.println("rows");
		int rows = Integer.parseInt(in.readLine());
		out.println("cols");
		int cols = Integer.parseInt(in.readLine());
		currentCells = new Cell[rows][cols];
		out.println("cells");
		while(true) {
			input = in.readLine();
			if (input.equals("done")) break;
			else {
				Cell tmp = new Cell(input);
				currentCells[(tmp.row % rows)][(tmp.col % cols)] = tmp;
			}
		}
		while (neighbourConnections.GetLiveConnections().size() < 8 
			&& neighbourConnections.GetLiveConnections().size() > 0);
		System.out.println("Worker (" + dataServer.getLocalSocketAddress() + ", " + workerPort + ") now has 8 inbound neighbour connections");
	}



	// Simply gets the port and host addresses of all this worker's 
	// neighbouring workers and then attempts to connect to them.
	private void GetNeighbours() throws IOException
	{
		String input = "";
		System.out.println("Getting neighbours...");
		out.println("neighbours");
		input = in.readLine();
		while(!input.equals("done")) {	
			SetupNeighbourConnection(input);
			input = in.readLine();
		}
	}



	// Connects to any available neighbours.
	private void SetupNeighbourConnection(String neighbourCSV) throws IOException
	{
		StringTokenizer st = new StringTokenizer(neighbourCSV, ",");
		if (st.countTokens() == 3) {
			String direction = st.nextToken().trim();
			String address = st.nextToken().trim();
			String port = st.nextToken().trim();
			
			System.out.println("Connecting to neighbour " + address + ", " + port + " to the " + direction);
			Socket conn = new Socket(address, Integer.parseInt(port));
			neighbours.put(direction, conn);
		}
	}



	// Now having received cells, enter the "gamestate"
	// loop:
	// - Get cells from neighbours.
	// - Make sure all 8 neighbours have received our boundaries.
	// - Get the next state and send the cells to the master.
	private void GameOfLifeLoop() throws IOException
	{
		String input = "";
		while(true) {
			out.println("ready");
			input = in.readLine();
			if (input.equals("nextState")) {
				Cell[][] tmpCells = GetNeighbourCells();

				while (GetReceivedNeighbours() < 8);

				if (tmpCells == null) currentCells = GameOfLifeUtils.NextState(currentCells);
				else NextState(tmpCells);

				out.println("cells");
				GameOfLifeUtils.PrintCells(currentCells, out);
				out.println("done");
			}
			else if (input.equals("GameOver")) break;
		}
	}



	// Count the number of updates that have been received by neighbours.
	private int GetReceivedNeighbours()
	{
		int count = 0;
		ArrayList<WorkerConnection> connections= neighbourConnections.GetLiveConnections();
		synchronized(connections) {
			for(WorkerConnection n : connections) {
				if (n.isDone()) count++;
			}
		}
		return count;
	}



	// REFACTOR ME - another class representing outbound connections.
	private ArrayList<Cell> GetNeighbourCellBoundary(Direction d) throws IOException
	{
		ArrayList<Cell> tmpList = new ArrayList();

		Socket sock = neighbours.get(d.toString());
		if (null != sock) {
			PrintWriter tmpOut = new PrintWriter(sock.getOutputStream(), true);
			BufferedReader tmpIn = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			tmpOut.println("getBoundary");
			tmpOut.println(d); 
			String input = "";	
			while(true) {
				input = tmpIn.readLine();
				if (input.equals("done")) break;
				else tmpList.add(new Cell(input));
			}
		}
		return tmpList;
	}



	// Get the neighbour cells and shove them into a border around the
	// set of cells received from the master.
	private Cell[][] GetNeighbourCells() throws IOException
	{
		Cell[][] borderCells = new Cell[currentCells.length+2][currentCells[0].length+2];

		for (int row = 0; row < currentCells.length; row++)
		{
			for (int col = 0; col < currentCells[row].length; col++)
				borderCells[row+1][col+1] = currentCells[row][col];
		}

		// REFACTOR ME
		for (Direction d: Direction.values())
		{
			ArrayList<Cell> tmpList = GetNeighbourCellBoundary(d);

			Iterator<Cell> i = tmpList.iterator();
			if (!i.hasNext()) return null;
			if (d == Direction.NORTH) {
				int c = 1;
				while(i.hasNext()) borderCells[0][c++] = i.next();
			} else if (d == Direction.NORTHEAST) {
				borderCells[0][borderCells[0].length-1] = i.next();
			} else if (d == Direction.EAST) {
				int r = 1;
				while(i.hasNext()) borderCells[r++][borderCells[0].length-1] = i.next();
			} else if (d == Direction.SOUTHEAST) {
				borderCells[borderCells.length-1][borderCells[0].length-1] = i.next();
			} else if (d == Direction.SOUTH) {
				int c = 1;
				while(i.hasNext()) borderCells[borderCells.length-1][c++] = i.next();
			} else if (d == Direction.SOUTHWEST) {
				borderCells[borderCells.length-1][0] = i.next();
			} else if (d == Direction.WEST) {
				int r = 1;
				while(i.hasNext()) borderCells[r++][0] = i.next();
			} else if (d == Direction.NORTHWEST) {
				borderCells[0][0] = i.next();
			}
		}
		return borderCells;
	}



	// Gets the boundary cells based on the direction from which the request is
	// coming. The boundary cells retrieved are those in the opposite direction
	// to the direction.
	public synchronized ArrayList<Cell> GetBoundaryCells(String direction)
	{
		ArrayList<Cell> list = new ArrayList();
		Direction dir = null;
		for (Direction d: Direction.values())
		{
			if (d.toString().equals(direction)) {
				dir = d;
				break;
			}
		}
		if (dir == null) return list;
		int row = dir.rows * -1;
		int col = dir.cols * -1;

		// HACK HACK HACK - ugly if-else chain.
		//
		// If row is -1 then top most row, if 1 bottom most.
		// Same for columns.
		if (row == -1 && col == -1) {
			list.add(currentCells[0][0]);
		} else if (row == -1 && col == 0) {
			for (int c = 0; c < currentCells[0].length; c++) 
				list.add(currentCells[0][c]);
		} else if (row == -1 && col == 1) {
			list.add(currentCells[0][currentCells[0].length-1]);
		} else if (row == 0 && col == -1) {
			for (int r = 0; r < currentCells.length; r++) 
				list.add(currentCells[r][0]);
		} else if (row == 0 && col == 1) {
			for (int r = 0; r < currentCells.length; r++) 
				list.add(currentCells[r][currentCells[0].length-1]);
		} else if (row == 1 && col == -1) {
			list.add(currentCells[currentCells.length-1][0]);
		} else if (row == 1 && col == 0) {
			for (int c = 0; c < currentCells[0].length; c++) 
				list.add(currentCells[currentCells.length-1][c]);
		} else if (row == 1 && col == 1) {
			list.add(currentCells[currentCells.length-1][currentCells[0].length-1]);
		}
		return list;
	}



	// Creates a temporary cell set that has a "border" - this is simply
	// loaded from the boundary cells. The next state of the game is
	// calculated from this set, and then the current set of cells are
	// extracted from this border (and will eventually be sent back to 
	// the master).
	private synchronized void NextState(Cell[][] temporaryCells) throws IOException
	{
		previousCells = currentCells;
		currentCells = new Cell[previousCells.length][previousCells[0].length];
		Cell[][] newCells = GameOfLifeUtils.NextState(temporaryCells);

		for (int row = 0; row < currentCells.length; row++)
		{
			for (int col = 0; col < currentCells[row].length; col++)
			{
				currentCells[row][col] = newCells[row+1][col+1];
			}
		}
	}



	private void CloseConnections() throws IOException
	{
		out.close();
		in.close();
		masterConnection.close();
	}



	public static void main(String[] args)
	{
		int wPort = Integer.parseInt(args[0]);
		String mHost = args[1];
		int mPort = Integer.parseInt(args[2]);
		
		Worker w = new Worker(wPort, mHost, mPort);
		try {
			w.EstablishConnections();
			w.GetSettings();
			w.GameOfLifeLoop();
			w.CloseConnections();
		} catch (IOException e) {
			System.err.println(e);
		}
	}
}




