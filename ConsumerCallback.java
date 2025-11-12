import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaz para callbacks de consumidor.
 * Permite al broker enviar mensajes al consumidor cuando están disponibles.
 */
public interface ConsumerCallback extends Remote {
    /**
     * Método llamado cuando hay un nuevo mensaje disponible.
     * El mensaje contiene el ID y el contenido separados por "||".
     * @param mensaje El mensaje recibido (formato: "ID||CONTENIDO")
     */
    void onMessage(String mensaje) throws RemoteException;
    
    /**
     * Método para obtener el ID único del consumidor
     * @return ID del consumidor
     */
    String getId() throws RemoteException;
}