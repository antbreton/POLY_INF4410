package tp2.repartiteur;


import java.util.HashMap;
import tp2.shared.ServerInterface;
import java.util.*;


public class Task {
	private ArrayList<String> operations = new ArrayList<>();
	private HashMap<ServerInterface, Integer> response = new HashMap<>();
	private HashMap<Integer, Integer> SameResponseCount = new HashMap<>();
	
	public void addOperation(String op){
		operations.add(op);
	}
	
	public String[] getOperations(){
		return operations.toArray(new String[0]);
	}
	
	public void addResponse(ServerInterface s, Integer result)
	{
		response.put(s,result);
		
		if(SameResponseCount.containsKey(result))
			SameResponseCount.put(result,SameResponseCount.get(result)+1);
		else
			SameResponseCount.put(result,1);
	}
	
	/**
	 * 
	 * Return the response if 2 servers answered the same result. Else return a error code -1.
	 * 
	 */ 
	public int getResponse(){
		Iterator it = SameResponseCount.entrySet().iterator();
		
		while(it.hasNext())
		{
			Map.Entry pair = (Map.Entry)it.next();
			if( ((int) pair.getValue()) == 2)
				return (int) pair.getKey();
		}
		return -1;
	}
	
}
