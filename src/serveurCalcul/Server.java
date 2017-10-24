package tp2.serveurCalcul;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

import tp2.shared.ServerInterface;
import tp2.serveurCalcul.Operations;

public class Server implements ServerInterface {
	
	private Integer seuilDeSurcharge;
	private Integer tauxDeMalice;
	private Random randomGen;

	public static void main(String[] args) {
		Integer surcharge = 1;
		Integer malice = 0;
		if (args.length > 0) {
			surcharge = new Integer(args[0]);
		}
		
		if (args.length > 1) {
			malice = new Integer(args[1]);
		}
		
		Server server = new Server(surcharge, malice);
		server.run();
	}

	public Server(Integer surcharge, Integer malice) {
		super();
		this.seuilDeSurcharge = surcharge;
		this.tauxDeMalice = malice;
		this.randomGen = new Random();
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}
	
	private Boolean refus(Integer length){
		if (randomGen.nextFloat() < (length - seuilDeSurcharge) / (5 * seuilDeSurcharge) ){
			return true;
		}
		return false;
	}
	
	private Boolean malicieux(){
		if (randomGen.nextInt() < tauxDeMalice ){
			return true;
		}
		return false;
	}
	
	/*
	 * Méthode accessible par RMI. Additionne les deux nombres passés en
	 * paramètre.
	 */
	@Override
	public int execute(String[] tache) throws RemoteException {
		Integer lengthOfTache = tache.length;
		if (refus(lengthOfTache)){
			return -1;
		}
		
		int resultat = 0;
		for (String operation: tache){
			String[] parsedOperation = operation.split(" ");
			if (parsedOperation[0] == "prime"){
				resultat = ( resultat + Operations.prime( Integer.parseInt(parsedOperation[1]) ) ) % 4000;
			} else if (parsedOperation[0] == "pell") {
				resultat = ( resultat + Operations.pell( Integer.parseInt(parsedOperation[1]) ) ) % 4000;
			} else {
				return -1;
			}
		}
		
		if (malicieux()){
			return randomGen.nextInt(4000);
		}
		return resultat;
	}
	
	@Override
	public int getSeuil() throws RemoteException {
		return seuilDeSurcharge;
	}
}
