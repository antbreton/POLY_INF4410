package tp2.repartiteur;


import java.util.HashMap;
import tp2.shared.ServerInterface;
import java.util.*;


public class Task {
	private ArrayList<String> operations = new ArrayList<>();
	private HashMap<ServerInterface, Integer> response = new HashMap<>();
	//Nombre de serveurs requis pour qu'une réponse soit acceptable.
	private int nbRequiredServers = 1;
	
	public Task(int nbRequiredServers){
		this.nbRequiredServers = nbRequiredServers;
	}
		
	public void addOperation(String op){
		operations.add(op);
	}
	
	public String[] getOperations(){
		return operations.toArray(new String[0]);
	}
	
	public void addResponse(ServerInterface s, Integer result)
	{
		response.put(s,result);
	}
	
	/**
	 * 
	 * Return the response if 2 servers answered the same result. Else return a error code -1.
	 * 
	 */ 
	public int getResponse(){
		
		// Compute sameResponseCount
		HashMap<Integer, Integer> SameResponseCount = new HashMap<>();
		Iterator responseIt = response.entrySet().iterator();
		
		while(responseIt.hasNext())
		{
			Map.Entry pair = (Map.Entry)responseIt.next();
			Integer result = (Integer) pair.getValue();
			// if the result is already in the map, then we increment it
			if(SameResponseCount.containsKey(result))
				SameResponseCount.put(result,SameResponseCount.get(result)+1);
			else // else we put it in the map
				SameResponseCount.put(result,1);
		}
		
		// Check if the response is acceptable
		Iterator countIt = SameResponseCount.entrySet().iterator();
		
		while(countIt.hasNext())
		{
			Map.Entry pair = (Map.Entry)countIt.next();
			// The response is acceptable if 2 servers gave the same answer.
			if( ((int) pair.getValue()) >= nbRequiredServers)
				return (int) pair.getKey();
		}
		return -1;
	}
	
}
