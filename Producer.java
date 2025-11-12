import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaz remota para el productor.
 */
public interface Producer extends Remote {
    /**
     * Retorna el nombre del productor.
     * @return Nombre del productor
     * @throws RemoteException Si hay error de comunicación RMI
     */
    String getName() throws RemoteException;
}