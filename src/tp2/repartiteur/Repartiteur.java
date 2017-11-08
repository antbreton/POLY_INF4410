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
	public int taskSize = 10;
	private Integer res = 0;
	private Object lock = new Object();
	private Boolean secured = true;
	
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
		
		//If Repartiteur is running in insecured mode, it will create tasks
		// with 2 required servers to accept response.
		Boolean secured = true;
		if (args.length > 2) {
			if (args[2].equals("insecured")){
				secured = false;
			} else {
				System.out.println("Erreur: Mode de sécurité invalide");
			}
		}

		Repartiteur repartiteur = new Repartiteur(confFilename, secured);
		repartiteur.run(opFilename);
	}

	public Repartiteur(String confFilename, Boolean secured) {
		
		this.secured = secured;
		
		// Get Server IPs from given configuration file
		String[] serverIPs = parseInputFile(confFilename);
		
		//Task have a maximum of 100 operations.
		int minSeuil = 100;
		// For each IP get the associated ServerIterface
		for (String ip : serverIPs) {
			ServerInterface curr = loadServerStub(ip);
			try{
			int currSeuil = curr.getSeuil();
			
			// get the minimum threshold of servers
			if(currSeuil < minSeuil)
				minSeuil = currSeuil;
			} catch(Exception e)
			{
				System.out.println("RMI getSeuil error");
			}
			
			// Add server in the list
			serveurs.add(curr);
		}
		
		// TaskSize computing
		System.out.println(minSeuil);
		taskSize = 3*minSeuil;
	}

	private void run(String filename) {
		
		// Get operations array
		String [] operations = parseInputFile(filename);

		// Get task Arrays 
		ArrayList <Task> tasks = getTaskArray(operations);
		ArrayList <Thread> threads = new ArrayList<>();
		
		checkIfNbServerIsEnough();
		
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
				if (secured){
					curr = new Task(1);
				}
				else {
					curr = new Task(2);
				}
				
				tasks.add(curr);
			}
			curr.addOperation(operations[i]);
		}
		
		return tasks;
	}
	
	private synchronized ServerInterface getNextServer()
	{
		// Acces concurrent qui doit être protégé par un sem
		checkIfNbServerIsEnough();
		ServerInterface next = serveurs.get(currServ%serveurs.size());
		currServ++;
		return next;
	}
		
	private Thread taskManagement(Task t)
	{
		Thread th = new Thread(new Runnable() {
			
			public void run ()
			{
				// get available servers
				ServerInterface s1 = getNextServer();
				// execute the task on the 2 previously choosen servers
				Thread t1 = AsyncRemoteTaskExecution(t, s1);
				
				//If in insecured mode, send task to a second server right away
				if (!secured){
					ServerInterface s2 = getNextServer();
					Thread t2 = AsyncRemoteTaskExecution(t, s2);
					try{
						t2.join();
					} catch (Exception e){
						//e.printStackTrace();
					}
				}
				
				try{
					t1.join();
				} catch (Exception e){
					//e.printStackTrace();
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
						//e.printStackTrace();
					}
				}
				
				// sem
				// Can't synchronize on res because of Integer pooling in java:
				// https://stackoverflow.com/questions/38117630/thread-synchronization-on-integer-instance-variable
				synchronized(lock){
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
							//System.out.println("Retour du serveur: " + result);
							if (result >= 0){
								tache.addResponse(s, result);
							}
						}
						
					} catch (RemoteException e) {
						//System.out.println("Erreur: " + e.getMessage());
						synchronized (serveurs){
							if(serveurs.contains(s)){
								System.out.println("Panne intempestive");
								managePanne(s);
							}
						}
					}	
				}
			});
			
		t.start();
		return t;	
	}
	
	private void managePanne(ServerInterface s){
		serveurs.remove(s);
		checkIfNbServerIsEnough();
	}
	
	private void checkIfNbServerIsEnough()
	{
		if (!secured && serveurs.size() < 2 || serveurs.size() == 0){
			System.out.println("Error : There are not enough availabled servers to execute task");
			
			System.exit(-1);
		}

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
			Registry registry = LocateRegistry.getRegistry(hostname, 5001);
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
