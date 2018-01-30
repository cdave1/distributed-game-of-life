// Copyright David Petrie 2008
//
// Handler for incoming connections to the master.
import java.io.*;
import java.net.*;
import java.util.*;

public class MasterConnectionHandler extends Thread
{
	public static Stack<MasterConnection> newConnections = new Stack();
	public static ArrayList<MasterConnection> liveConnections = new ArrayList();
	public static ServerSocket server = null;
	public static int connectionCount = 0;

	public MasterConnectionHandler(ServerSocket server)
	{
		this.server = server;		
	}



	// Blocks until something received on port.
	public void run()
	{
		try {
			while(true) {
				Socket worker = server.accept();
				MasterConnection w = new MasterConnection(worker, connectionCount++);
				newConnections.push(w);
			}
		} catch (IOException e) {
			System.err.println(e);
		}
	}



	public static void StartNewConnections()
	{
		MasterConnection tmp = null;
		
		while(!newConnections.empty())
		{
			tmp = newConnections.pop();
			tmp.start();
			while(tmp.isReady() == false);
			liveConnections.add(tmp);
		}
	}



	public static ArrayList<MasterConnection> GetLiveConnections()
	{
		return liveConnections;
	}
}
