package tp2.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
	int execute(String[] tache) throws RemoteException;
}
