package tp2.repartiteur;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import java.lang.Thread;
import java.lang.reflect.Array;
import java.lang.Runnable;
import java.util.*;
import java.io.*;

import tp2.shared.ServerInterface;

public class Repartiteur {
	
	private ArrayList<ServerInterface> serveurs = new ArrayList<>();
	private int currServ = 0;
	public static int taskSize = 10;
	private Integer res = 0;
	
	public static void main(String[] args) {
		String opFilename = null;
		String confFilename = null;
		
		if (args.length > 0) {
			opFilename = args[0];
		}
		
		if (args.length > 1) {
			confFilename = args[1];
		} else {
			confFilename = "ips.txt";
		}

		Repartiteur repartiteur = new Repartiteur(confFilename);
		repartiteur.run(opFilename);
	}

	public Repartiteur(String confFilename) {
		
		// Get Server IPs from given configuration file
		String[] serverIPs = parseInputFile(confFilename);
		
		// For each IP get the associated ServerIterface
		for (String ip : serverIPs) {
			serveurs.add(loadServerStub(ip));
		}
	}

	private void run(String filename) {
		
		// Get operations array
		String [] operations = parseInputFile(filename);

		// Get task Arrays 
		ArrayList <Task> tasks = getTaskArray(operations);
		ArrayList <Thread> threads = new ArrayList<>();
		
		// send tasks 
		for(Task t : tasks)
		{
			threads.add(taskManagement(t));
		}
		
		//Wait for all threads to finish
		for(Thread th : threads){
			try{
				th.join();
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		
		// print result
		System.out.println(res);
	}
	
	private ArrayList<Task> getTaskArray(String [] operations)
	{
		ArrayList<Task> tasks = new ArrayList<>();
		
		Task curr = null;
		for(int i =0; i<Array.getLength(operations); i++)
		{
			if(i%taskSize == 0)
			{
				curr = new Task();
				tasks.add(curr);
			}
			curr.addOperation(operations[i]);
		}
		
		return tasks;
	}
	
	private synchronized ServerInterface getNextServer()
	{
		// Acces concurrent qui doit être protégé par un sem
		ServerInterface next = serveurs.get(currServ%serveurs.size()-1);
		currServ++;
		return next;
	}
		
	private Thread taskManagement(Task t)
	{
		Thread th = new Thread(new Runnable() {
			
			public void run ()
			{
				// get 2 available servers
				ServerInterface s1 = getNextServer();
				ServerInterface s2 = getNextServer();
				
				// execute the task on the 2 previously choosen servers
				Thread t1 = AsyncRemoteTaskExecution(t, s1);
				Thread t2 = AsyncRemoteTaskExecution(t, s2);
				
				try{
					t1.join();
					t2.join();
				} catch (Exception e){
					e.printStackTrace();
				}
				
				// while the task response is not acceptable
				while(t.getResponse() == -1) // maybe put a timeout ?
				{
					// we send the request to another server
					s1 = getNextServer();
					t1 = AsyncRemoteTaskExecution(t, s1);
					try{
						t1.join();
					} catch (Exception e){
						e.printStackTrace();
					}
				}
				
				// sem
				synchronized (res){
					res = (res + t.getResponse()) % 4000;
				}
			}
		});
			
		th.start();
		return th;
	}
	
	private Thread AsyncRemoteTaskExecution(Task tache, ServerInterface s)
	{
		Thread t = new Thread(new Runnable() {
				public void run()
				{
					try {
						int result = s.execute(tache.getOperations());
						
						// Semaphore pour ajouter la reponse
						synchronized (tache){
							tache.addResponse(s, result);
						}
						
					} catch (RemoteException e) {
						System.out.println("Erreur: " + e.getMessage());
					}	
				}
			});
			
		t.start();
		return t;	
	}
	
	// Get a String array of operation from a file
	private String[] parseInputFile(String filename)
	{
		ArrayList<String> parsedLines = new ArrayList <>();
		
		// Open the file
		try {
			FileInputStream fstream = new FileInputStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

			String strLine;

			//Read File Line By Line
			while ((strLine = br.readLine()) != null)   {
			  // Print the content on the console
			  parsedLines.add(strLine);
			}

			//Close the input stream
			br.close();
		} catch (Exception e) {
			System.out.println("Erreur: " + e.getMessage());
		}	
		
		return parsedLines.toArray(new String[0]);
	}
	
	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}
}
