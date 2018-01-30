// Copyright David Petrie 2008
//
// Handler for each worker to handle incoming connections from other
// neighbours.
import java.io.*;
import java.net.*;
import java.util.*;

public class WorkerConnectionHandler extends Thread
{
	public ArrayList<WorkerConnection> liveConnections = new ArrayList();
	public ServerSocket socket = null;
	public Worker worker;

	public WorkerConnectionHandler(Worker worker, ServerSocket socket)
	{
		this.worker = worker;
		this.socket = socket;
	}


	// Collect incoming worker connection attempts.
	public void run()
	{
		try {
			while(true) {
				Socket neighbour = socket.accept(); //blocks until connection comes through
				System.out.println("Connection accepted " + neighbour);
				WorkerConnection w = new WorkerConnection(worker, neighbour);
				w.start();
				while(w.isReady() == false);
				synchronized(liveConnections) {
					liveConnections.add(w);
				}
			}
		} catch (IOException e) {
			System.err.println(e);
		}
	}



	public ArrayList<WorkerConnection> GetLiveConnections()
	{
		return liveConnections;
	}
}
