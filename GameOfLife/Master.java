// Copyright David Petrie 2008
//
// Master - main controller for distributed game-of-life program.
//
// Incoming connections are handled with an instance of the 
// MasterConnectionHandler running in its own thread.
import java.net.*;
import java.util.*;
import java.io.*;



public class Master {
	public static int port = 12345;
	public static int generations = 10;
	public static String patternFile = "";
	public static int workers = 1;
	public static ServerSocket server = null;
	public static MasterConnectionHandler masterConnections = null;
	public static Cell[][] previousCells = null;
	public static Cell[][] currentCells = null;


	// Setup workers then run the game of life. We simply wait until an 
	// expected number of workers attempt to connect.
	public static void SetupWorkers() throws IOException
	{
		while(masterConnections.newConnections.size() < workers); 
		masterConnections.StartNewConnections();
		while(masterConnections.liveConnections.size() < workers);
		ShowNeighbours();
	}



	// Send cells down each of the available connections...
	public static void SendCells() throws IOException
	{
		ArrayList<MasterConnection> liveConnections = masterConnections.GetLiveConnections();
		for (MasterConnection w: liveConnections)
			w.SendWorkInformation(GenerateWorkerCells(liveConnections.size(), w.GetID()));
	}



	// Loop until generation limit reached. Do
	// the following:
	// - Send a heartbeat to each worker to start the next state.
	// - Collect the cells from each worker.
	// - Break if cells identical. 
	public static void GameOfLifeLoop() throws IOException
	{
		ArrayList<MasterConnection> liveConnections = masterConnections.GetLiveConnections();
		int i = 0;
		while(i < generations) {
			for (MasterConnection w: liveConnections)
			{
				w.SetNextState();
			}
			previousCells = currentCells;
			currentCells = new Cell[currentCells.length][currentCells[0].length];
			for (MasterConnection w: liveConnections)
			{
				ArrayList<Cell> list = w.GetCells();
				for(Cell c: list) currentCells[c.row][c.col] = c;
			}
			System.out.println("Cells at " + i);
			GameOfLifeUtils.PrintCells(currentCells);
			if (GameOfLifeUtils.CompareCells(previousCells, currentCells)) break;
			i++;
		}
	}



	// Shows neighbours for each worker
	public static void ShowNeighbours() throws IOException
	{
		ArrayList<MasterConnection> liveConnections = masterConnections.GetLiveConnections();
		for (MasterConnection w: liveConnections)
		{
			System.out.println("Neighbours for worker " + w.GetID());
			for (Direction d: Direction.values()) {
				MasterConnection neighbour = GetNeighbour(liveConnections.size(), w.GetID(), d);
				if (neighbour == null) System.out.println("NONE at " + d);
				else System.out.println(neighbour.GetInfo() + " at " + d);
			}
		}
	}



	// Get possible neighbours for a worker.
	public static int GetPossibleNeighbours(MasterConnection w) throws IOException
	{
		int count = 0;
		if (masterConnections.GetLiveConnections().size() == 1) return 8;
		for (Direction d: Direction.values()) {
			MasterConnection wc = GetNeighbour(w, d);
			if (wc != null) count++;
		}
		return count;
	}



	// Gets the neighbouring worker for w in the given direction.
	public static MasterConnection GetNeighbour(MasterConnection w, Direction d) throws IOException
	{
		MasterConnection neighbour = GetNeighbour(masterConnections.GetLiveConnections().size(), w.GetID(), d);
		if (neighbour == null) return w;
		return neighbour;
	}



	// Generates a subset of cells and gives them to the worker.
	public static Cell[][] GenerateWorkerCells(int workers, int workerID)
	{	
		int rows = GameOfLifeUtils.GetWorkerPatternRows(workers);
		int cols = GameOfLifeUtils.GetWorkerPatternCols(workers);
		int workerRow = GameOfLifeUtils.GetWorkerRow(workers, workerID);
		int workerCol = GameOfLifeUtils.GetWorkerCol(workers, workerID);

		int rowCells = currentCells.length / rows;
		int colCells = currentCells[0].length / cols;
		
		Cell[][] subCells = new Cell[rowCells][colCells];

		for (int row = 0; row < rowCells; row++)
		{
			for (int col = 0; col < colCells; col++)
			{
				subCells[row][col] = currentCells[row+(workerRow * rowCells)][col+(workerCol * colCells)];
			}
		}

		return subCells;
	}



	// Gets the topology of the worker pattern, then
	// finds the worker in direction d from the worker with workerID.
	public static MasterConnection GetNeighbour(int workers, int workerID, Direction d)
	{
		int rows = GameOfLifeUtils.GetWorkerPatternRows(workers);
		int cols = GameOfLifeUtils.GetWorkerPatternCols(workers);
		int workerRow = (GameOfLifeUtils.GetWorkerRow(workers, workerID) + d.rows);
		int workerCol = (GameOfLifeUtils.GetWorkerCol(workers, workerID) + d.cols);

		if (workerRow < 0) workerRow += rows;
		else workerRow %= rows;
		if (workerCol < 0) workerCol += cols;
		else workerCol %= cols;

		ArrayList<MasterConnection> liveConnections = masterConnections.GetLiveConnections();
		for (MasterConnection w: liveConnections)
		{
			synchronized(w) {
				if ((workerRow == GameOfLifeUtils.GetWorkerRow(liveConnections.size(), w.GetID())) &&
					workerCol == GameOfLifeUtils.GetWorkerCol(liveConnections.size(), w.GetID())) return w;
			}
		}
		return null;
	}



	public static void CloseChannels() throws IOException
	{
		for (MasterConnection w: masterConnections.GetLiveConnections())
			w.CloseChannels();
		server.close();		
	}



	public static void main(String[] args)
	{
		port = Integer.parseInt(args[0]);
		generations = Integer.parseInt(args[1]);
		patternFile = args[2];
		workers = Integer.parseInt(args[3]);
		
		Cell[][] pattern = GameOfLifeUtils.LoadPattern(patternFile);
		GameOfLifeUtils.PrintCells(pattern);
		currentCells = GameOfLifeUtils.GetGameOfLifeSpace(workers, 20, pattern);

		try {
			server = new ServerSocket(port);
			masterConnections = new MasterConnectionHandler(server);
			masterConnections.start();

			SetupWorkers();
			SendCells();
			GameOfLifeLoop();
			CloseChannels();
		} catch (IOException e)
		{
			System.err.println(e);
		}
	}
}
