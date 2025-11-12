import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaz remota para el consumidor.
 */
public interface Consumer extends Remote {
    /**
     * Retorna el ID único del consumidor.
     * @return ID del consumidor
     * @throws RemoteException Si hay error de comunicación RMI
     */
    String getId() throws RemoteException;
}