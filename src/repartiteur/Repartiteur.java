package tp2.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import tp2.shared.ServerInterface;

public class Repartiteur {
	
	private ArrayList<ServerInterface> serveurs = new ArrayList<>();
	private int currServ = 0;
	public static int taskSize = 10;
	private int res = 0;
	
	public static void main(String[] args) {
		String fileName = null;

		if (args.length > 0) {
			fileName = args[0];
		}

		Repartiteur repartiteur = new Repartiteur();
		repartiteur.run(filename);
	}

	public Repartiteur() {
	
		// Add servers in the list
	}

	private void run() {
		
		// Get operations array
		String [] operations = parseInputFile(filename);

		// Get task Arrays 
		ArrayList <Task> tasks = getTaskArray(operations);
		
		// send tasks 
		for(Task t : tasks)
		{
			taskManagement(t);
		}
		
		// print result
	}
	
	private ArrayList<Task> getTaskArray(String [] operations)
	{
		ArrayList<Task> tasks = new ArrayList<>();
		
		Task curr;
		for(int i =0; i<operations.length(); i++)
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
	
	private ServerInterface getNextServer()
	{
		// Acces concurrent qui doit être protégé par un sem
		ServeurInterface next = serveurs.get(currServ%serveurs.size()-1);
		currServ++;
		return next;
	}
		
	private void taskManagement(Tache t)
	{
		Thread t = new Runnable() {
			// get 2 available servers
			ServeurInterface s1 = getNextServer();
			ServeurInterface s2 = getNextServer();
			
			// execute the task on the 2 previously choosen servers
			Thread t1 = AsyncRemoteTaskExecution(t, s1);
			Thread t2 = AsyncRemoteTaskExecution(t, s2);
			
			// wait for the threads to finish
			t1.join();
			t2.join();
			
			// while the task response is not acceptable
			while(!t.isResponseAcceptable()) // maybe put a timeout ?
			{
				// we send the request to another server
				s1 = getNextServer();
				t1 = AsyncRemoteTaskExecution(t, s1);
				t1.join();
			}
			
			// sem
			res = (res + t.getAnswer()) % 4000;
		};
			
		t.start();
	}
	
	private Thread AsyncRemoteTaskExecution(Tache tache, ServerInterface s)
	{
		Thread t = new Runnable() {
				void run()
				{
					try {
						int result = s.execute(tache.getOperations());
						
						// Semaphore pour ajouter la reponse
						tache.addResponse(result);
						
						
					} catch (RemoteException e) {
						System.out.println("Erreur: " + e.getMessage());
					}	
				}
			};
			
		t.start();
		return t;	
	}
	
	// Get a String array of operation from a file
	private String[] parseInputFile(filename)
	{
		ArrayList<String> parsedLines = new ArrayList <>();
		
		// Open the file
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
		
		return parsedLines.toArray();
	}
}
