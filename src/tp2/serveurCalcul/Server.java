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
	private Integer tauxDePanne;
	private Random randomGen;

	public static void main(String[] args) {
		Integer surcharge = 1;
		Integer malice = 0;
		Integer panne = 0;
		Integer port = 1099;
		if (args.length > 0) {
			surcharge = new Integer(args[0]);
		}
		
		if (args.length > 1) {
			malice = new Integer(args[1]);
		}
		
		if (args.length > 2) {
			panne = new Integer(args[2]);
		}
		
		if (args.length > 3) {
			port = new Integer(args[3]);
		}
		
		
		Server server = new Server(surcharge, malice, panne);
		server.run(port);
	}

	public Server(Integer surcharge, Integer malice, Integer panne) {
		super();
		this.seuilDeSurcharge = surcharge;
		this.tauxDeMalice = malice;
		this.tauxDePanne = panne;
		System.out.println("Initialisation avec surcharge=" + this.seuilDeSurcharge + " et malice=" + this.tauxDeMalice + "\"");
		this.randomGen = new Random();
	}

	private void run(int port) {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry(port);
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
		if (randomGen.nextFloat() < (new Float(length - seuilDeSurcharge)) / (5.0f * seuilDeSurcharge) ){
			return true;
		}
		return false;
	}
	
	private Boolean malicieux(){
		if (randomGen.nextInt(100) < tauxDeMalice ){
			return true;
		}
		return false;
	}
	
	private void panne(){
		if (randomGen.nextInt(100) < tauxDePanne ){
			System.exit(0);
		}
	}
	
	/*
	 * Méthode accessible par RMI. Additionne les deux nombres passés en
	 * paramètre.
	 */
	@Override
	public int execute(String[] tache) throws RemoteException {
		
		Integer lengthOfTache = tache.length;
		if (refus(lengthOfTache)){
			//System.out.println("Refus pour longueur de tache: " + lengthOfTache);
			return -1;
		}
		
		
		int resultat = 0;
		for (String operation: tache){
			//System.out.println("Début de l'opération: " + operation);
			String[] parsedOperation = operation.split(" ");
			if (parsedOperation[0].equals("prime")){
				resultat = ( resultat + Operations.prime( Integer.parseInt(parsedOperation[1]) ) ) % 4000;
			} else if (parsedOperation[0].equals("pell")) {
				resultat = ( resultat + Operations.pell( Integer.parseInt(parsedOperation[1]) ) ) % 4000;
			} else {
				return -1;
			}
		}
		
		if (malicieux()){
			//System.out.println("MALICIEUX");
			return randomGen.nextInt(4000);
		}

		panne();
		//System.out.println("Retour du résultat: " + resultat);
		return resultat;
	}
	
	@Override
	public int getSeuil() throws RemoteException {
		return seuilDeSurcharge;
	}
}
