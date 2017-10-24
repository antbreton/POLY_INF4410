package tp2.client;

import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import tp2.ServerInterface;

public class Repartiteur {
	public static void main(String[] args) {
		String distantHostname = null;

		if (args.length > 0) {
			distantHostname = args[0];
		}
			
		
		int xTmp =1;
		if (args.length > 1) {
			xTmp = new Integer(args[1]);
		}
		
		Repartiteur repartiteur = new Repartiteur(distantHostname,xTmp);
		Repartiteur.run();
	}

	private ServerInterface distantServerStub = null;

	public Repartiteur(String distantServerHostname, int x) {
		super();

		this.x=x;
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		localServer = new FakeServer();
		localServerStub = loadServerStub("127.0.0.1");

		if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}
	}

	private void run() {
		
		if (distantServerStub != null) {
			appelRMIDistant();
		}
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


	private void appelRMIDistant() {
		try {
			long start = System.nanoTime();
			int result = distantServerStub.execute(new byte[((int)Math.pow(10,x))]);
			long end = System.nanoTime();
			System.out.print((end - start)+"\n");
			
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}
}
