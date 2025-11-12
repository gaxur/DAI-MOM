import java.rmi.Remote;
import java.util.List;
import java.rmi.RemoteException;

/**
 * Interfaz remota que define las operaciones básicas del broker de mensajes.
 */
public interface MessageBroker extends Remote {
    
    /**
     * Crea una nueva cola si no existe (operación idempotente)
     * @param nombreCola Identificador único de la cola
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    void declararCola(String nombreCola) throws RemoteException;
    
    /**
     * Crea una nueva cola con opciones de durabilidad
     * @param nombreCola Identificador único de la cola
     * @param durable Si la cola debe persistir
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    void declararCola(String nombreCola, boolean durable) throws RemoteException;
    
    /**
     * Elimina una cola existente
     * @param nombreCola Nombre de la cola a eliminar
     * @return true si se eliminó correctamente
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    boolean eliminarCola(String nombreCola) throws RemoteException;
    
    /**
     * Publica un mensaje en una cola existente
     * @param nombreCola Cola destino
     * @param mensaje Contenido del mensaje
     * @return true si el mensaje fue aceptado, false si la cola no existe
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    boolean publicar(String nombreCola, String mensaje) throws RemoteException;
    
    /**
     * Publica un mensaje en una cola con opciones de durabilidad
     * @param nombreCola Cola destino
     * @param mensaje Contenido del mensaje
     * @param durable Si el mensaje debe persistir
     * @return true si el mensaje fue aceptado, false si la cola no existe
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    boolean publicar(String nombreCola, String mensaje, boolean durable) throws RemoteException;
    
    /**
     * Registra un consumidor para recibir mensajes de una cola
     * @param nombreCola Cola a suscribirse
     * @param callback Función de procesamiento de mensajes
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    void consumir(String nombreCola, ConsumerCallback callback) throws RemoteException;
    
    /**
     * Desuscribe un consumidor de una cola
     * @param nombreCola Cola a desuscribirse
     * @param callback Consumidor a desuscribir
     * @return true si se desuscribió correctamente
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    boolean desuscribir(String nombreCola, ConsumerCallback callback) throws RemoteException;
    
    /**
     * Establece el modo de distribución fair
     * @param nombreCola Nombre de la cola
     * @param fair true para activar fair dispatch
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    void setFairDispatch(String nombreCola, boolean fair) throws RemoteException;
    
    /**
     * Confirma el procesamiento de un mensaje (ACK)
     * @param nombreCola Nombre de la cola
     * @param mensajeId ID del mensaje
     * @param callback Consumidor que confirma
     * @return true si se confirmó correctamente
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    boolean confirmarMensaje(String nombreCola, String mensajeId, ConsumerCallback callback) throws RemoteException;
    
    /**
     * Rechaza un mensaje (NACK)
     * @param nombreCola Nombre de la cola
     * @param mensajeId ID del mensaje
     * @param callback Consumidor que rechaza
     * @return true si se rechazó correctamente
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    boolean rechazarMensaje(String nombreCola, String mensajeId, ConsumerCallback callback) throws RemoteException;

    /**
     * Retorna una lista con los nombres de todas las colas disponibles
     * @return Lista de nombres de colas
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    List<String> listarColas() throws RemoteException;
    
    /**
     * Obtiene información sobre una cola específica
     * @param nombreCola Nombre de la cola
     * @return Información formateada como string
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    String getInfoCola(String nombreCola) throws RemoteException;
    
    /**
     * Agrega un agente de filtrado al sistema
     * @param agente Agente a agregar
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    void agregarAgente(MessageFilterAgent agente) throws RemoteException;
    
    /**
     * Elimina un agente del sistema
     * @param nombreAgente Nombre del agente a eliminar
     * @return true si se eliminó correctamente
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    boolean eliminarAgente(String nombreAgente) throws RemoteException;
    
    /**
     * Lista todos los agentes activos
     * @return Lista de agentes
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    List<MessageFilterAgent> listarAgentes() throws RemoteException;
    
    /**
     * Habilita o deshabilita el sistema de agentes
     * @param habilitado true para habilitar, false para deshabilitar
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    void setAgentesHabilitados(boolean habilitado) throws RemoteException;
    
    /**
     * Verifica si el sistema de agentes está habilitado
     * @return true si está habilitado
     * @throws RemoteException Si ocurre un error en la comunicación remota
     */
    boolean isAgentesHabilitados() throws RemoteException;
}