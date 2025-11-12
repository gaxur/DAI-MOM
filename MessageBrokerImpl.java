import java.util.concurrent.*;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.io.Serializable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del broker de mensajes que utiliza RMI para comunicación remota.
 * Gestiona todas las colas de mensajes y sus operaciones básicas.
 */
public class MessageBrokerImpl extends UnicastRemoteObject implements MessageBroker, Serializable {
    private static final long serialVersionUID = 1L;
    private static MessageBroker instance;
    private final ConcurrentMap<String, MessageQueue> queues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Sistema de agentes para filtrado de mensajes
    private final AgentManager agentManager = new AgentManager();
    
    // Canales predeterminados
    private static final String[] CANALES_PREDETERMINADOS = {
        "canal_NOTIFICACION", 
        "canal_ALERTA", 
        "canal_INFO", 
        "canal_GENERAL"
    };
    
    // Indica si estos canales serán durables
    private static final boolean CANALES_DURABLES = true;

    /**
     * Constructor del MessageBroker
     */
    protected MessageBrokerImpl() throws RemoteException {
        super();
        // Programa limpieza periódica de mensajes expirados cada 30 segundos
        scheduler.scheduleAtFixedRate(this::cleanExpiredMessages, 0, 30, TimeUnit.SECONDS);
        
        // Inicializa las colas predeterminadas
        inicializarColasPredeteminadas();
        
        // Recuperar colas durables
        recuperarColasDurables();
        
        // Configurar agentes por defecto
        agentManager.configurarAgentesPorDefecto();
    }

    // Limpia mensajes expirados en todas las colas
    private void cleanExpiredMessages() {
        System.out.println("");
        System.out.println("-----------------------------------");
        queues.forEach((nombre, cola) -> {
            System.out.println("Checking queue '" + nombre + 
                            "' (Messages: " + cola.contarMensajes() + 
                            ", Consumers: " + cola.contarConsumidores() + ")");
            cola.eliminarMensajesExpirados();
            System.out.println("-----------------------------------");
        });
    }
    
    /**
     * Inicializa las colas predeterminadas del sistema
     */
    private void inicializarColasPredeteminadas() {
        try {
            for (String canal : CANALES_PREDETERMINADOS) {
                declararCola(canal, CANALES_DURABLES);
                System.out.println("Default channel '" + canal + "' initialized (durable: " 
                                 + CANALES_DURABLES + ")");
            }
        } catch (Exception e) {
            System.err.println("Error initializing default channels: " + e.getMessage());
        }
    }
    
    /**
     * Recupera colas durables del sistema
     */
    private void recuperarColasDurables() {
        File dir = new File(".");
        File[] files = dir.listFiles((d, name) -> name.startsWith("queue_") && name.endsWith(".dat"));
        
        if (files == null || files.length == 0) {
            System.out.println("No durable queue files found to recover.");
            return;
        }
        
        for (File file : files) {
            try {
                String nombreCola = file.getName().substring(6, file.getName().length() - 4);
                
                // Si la cola no existe ya en memoria, crearla
                if (!queues.containsKey(nombreCola)) {
                    declararCola(nombreCola, true);
                    System.out.println("Durable queue '" + nombreCola + "' recovered from disk");
                }
            } catch (Exception e) {
                System.err.println("Error recovering queue from file " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Obtiene la instancia única del MessageBroker (patrón Singleton)
     */
    public static synchronized MessageBroker getInstance() throws RemoteException {
        if (instance == null) {
            instance = new MessageBrokerImpl();
        }
        return instance;
    }

    /**
     * Crea una nueva cola si no existe (operación idempotente)
     * @param nombreCola Identificador único de la cola
     */
    @Override
    public void declararCola(String nombreCola) throws RemoteException {
        declararCola(nombreCola, false);
    }
    
    /**
     * Crea una nueva cola con opciones de durabilidad
     * @param nombreCola Identificador único de la cola
     * @param durable Si la cola debe persistir
     */
    @Override
    public void declararCola(String nombreCola, boolean durable) throws RemoteException {
        queues.computeIfAbsent(nombreCola, k -> new MessageQueue(nombreCola, durable));
        System.out.println("Queue declared: " + nombreCola + " (durable: " + durable + ")");
    }
    
    /**
     * Elimina una cola existente
     * @param nombreCola Nombre de la cola a eliminar
     * @return true si se eliminó correctamente
     */
    @Override
    public boolean eliminarCola(String nombreCola) throws RemoteException {
        MessageQueue cola = queues.remove(nombreCola);
        if (cola != null) {
            cola.eliminar();
            System.out.println("Queue '" + nombreCola + "' removed from the broker");
            return true;
        }
        return false;
    }

    /**
     * Publica un mensaje en una cola existente
     * @param nombreCola Cola destino
     * @param mensaje Contenido del mensaje
     * @return true si el mensaje fue aceptado, false si la cola no existe
     */
    @Override
    public boolean publicar(String nombreCola, String mensaje) throws RemoteException {
        MessageQueue queue = queues.get(nombreCola);
        if (queue == null) return false;
        
        // Evaluar mensaje con agentes IA
        if (!agentManager.evaluarMensaje(mensaje, nombreCola)) {
            System.out.println("Message rejected by agents and NOT published to the queue '" + nombreCola + "'");
            return false;
        }
        
        queue.publicar(mensaje);
        System.out.println("Message published to the queue '" + nombreCola + "': " + mensaje);
        return true;
    }
    
    /**
     * Publica un mensaje en una cola con opciones de durabilidad
     * @param nombreCola Cola destino
     * @param mensaje Contenido del mensaje
     * @param durable Si el mensaje debe persistir
     * @return true si el mensaje fue aceptado, false si la cola no existe
     */
    @Override
    public boolean publicar(String nombreCola, String mensaje, boolean durable) throws RemoteException {
        MessageQueue queue = queues.get(nombreCola);
        if (queue == null) return false;
        
        // Evaluar mensaje con agentes IA
        if (!agentManager.evaluarMensaje(mensaje, nombreCola)) {
            System.out.println("Message rejected by agents and NOT published to the queue '" + nombreCola + "'");
            return false;
        }
        
        queue.publicar(mensaje, durable);
        System.out.println("Message published to the queue '" + nombreCola + "' (durable: " + durable + "): " + mensaje);
        return true;
    }

    /**
     * Registra un consumidor para recibir mensajes de una cola
     * @param nombreCola Cola a suscribirse
     * @param callback Función de procesamiento de mensajes
     */
    @Override
    public void consumir(String nombreCola, ConsumerCallback callback) throws RemoteException {
        MessageQueue queue = queues.get(nombreCola);
        if (queue != null) {
            queue.registrarConsumidor(callback);
            System.out.println("Consumer registered for queue: " + nombreCola);
        } else {
            System.out.println("Error: attempt to consume from non-existent queue: " + nombreCola);
        }
    }
    
    /**
     * Desuscribe un consumidor de una cola
     * @param nombreCola Cola a desuscribirse
     * @param callback Consumidor a desuscribir
     * @return true si se desuscribió correctamente
     */
    @Override
    public boolean desuscribir(String nombreCola, ConsumerCallback callback) throws RemoteException {
        MessageQueue queue = queues.get(nombreCola);
        if (queue != null) {
            boolean result = queue.desuscribirConsumidor(callback);
            if (result) {
                System.out.println("Consumer unsubscribed from queue: " + nombreCola);
            }
            return result;
        }
        return false;
    }
    
    /**
     * Establece el modo de distribución fair
     * @param nombreCola Nombre de la cola
     * @param fair true para activar fair dispatch
     */
    @Override
    public void setFairDispatch(String nombreCola, boolean fair) throws RemoteException {
        MessageQueue queue = queues.get(nombreCola);
        if (queue != null) {
            queue.setFairDispatch(fair);
        } else {
            System.out.println("Error: attempt to configure non-existent queue: " + nombreCola);
        }
    }
    
    /**
     * Confirma el procesamiento de un mensaje (ACK)
     * @param nombreCola Nombre de la cola
     * @param mensajeId ID del mensaje
     * @param callback Consumidor que confirma
     * @return true si se confirmó correctamente
     */
    @Override
    public boolean confirmarMensaje(String nombreCola, String mensajeId, ConsumerCallback callback) 
            throws RemoteException {
        MessageQueue queue = queues.get(nombreCola);
        if (queue != null) {
            return queue.confirmarMensaje(mensajeId, callback);
        }
        return false;
    }
    
    /**
     * Rechaza un mensaje (NACK)
     * @param nombreCola Nombre de la cola
     * @param mensajeId ID del mensaje
     * @param callback Consumidor que rechaza
     * @return true si se rechazó correctamente
     */
    @Override
    public boolean rechazarMensaje(String nombreCola, String mensajeId, ConsumerCallback callback) 
            throws RemoteException {
        MessageQueue queue = queues.get(nombreCola);
        if (queue != null) {
            return queue.rechazarMensaje(mensajeId, callback);
        }
        return false;
    }
    
    /**
     * Retorna una lista con los nombres de todas las colas disponibles
     * @return Lista de nombres de colas
     */
    @Override
    public List<String> listarColas() throws RemoteException {
        return new ArrayList<>(queues.keySet());
    }
    
    /**
     * Obtiene información sobre una cola específica
     * @param nombreCola Nombre de la cola
     * @return Información formateada como string
     */
    @Override
    public String getInfoCola(String nombreCola) throws RemoteException {
        MessageQueue queue = queues.get(nombreCola);
        if (queue == null) {
            return "Queue '" + nombreCola + "' does not exist";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Queue: ").append(nombreCola).append("\n");
        info.append("Durable: ").append(queue.esDurable()).append("\n");
        info.append("Messages: ").append(queue.contarMensajes()).append("\n");
        info.append("Consumers: ").append(queue.contarConsumidores()).append("\n");
        
        return info.toString();
    }
    
    /**
     * Agrega un agente de filtrado al sistema
     * @param agente Agente a agregar
     */
    @Override
    public void agregarAgente(MessageFilterAgent agente) throws RemoteException {
        agentManager.agregarAgente(agente);
    }
    
    /**
     * Elimina un agente del sistema
     * @param nombreAgente Nombre del agente a eliminar
     * @return true si se eliminó correctamente
     */
    @Override
    public boolean eliminarAgente(String nombreAgente) throws RemoteException {
        boolean eliminado = agentManager.eliminarAgente(nombreAgente);
        if (eliminado) {
            System.out.println("Agent '" + nombreAgente + "' removed from the system");
        } else {
            System.out.println("Agent '" + nombreAgente + "' not found");
        }
        return eliminado;
    }
    
    /**
     * Lista todos los agentes activos
     * @return Lista de agentes
     */
    @Override
    public List<MessageFilterAgent> listarAgentes() throws RemoteException {
        return agentManager.listarAgentes();
    }
    
    /**
     * Habilita o deshabilita el sistema de agentes
     * @param habilitado true para habilitar, false para deshabilitar
     */
    @Override
    public void setAgentesHabilitados(boolean habilitado) throws RemoteException {
        agentManager.setHabilitado(habilitado);
    }
    
    /**
     * Verifica si el sistema de agentes está habilitado
     * @return true si está habilitado
     */
    @Override
    public boolean isAgentesHabilitados() throws RemoteException {
        return agentManager.isHabilitado();
    }
    
    /**
     * Método principal que inicia el broker.
     * @param args Array con la URL donde se registrará el broker
     */
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.err.println("A parameter is required: the MessageBroker URL.");
                System.err.println("Example: java MessageBrokerImpl rmi://localhost/MessageBroker");
                System.exit(1);
            }

            // Obtener la instancia del broker (los canales se inicializan en el constructor)
            MessageBroker broker = MessageBrokerImpl.getInstance();
            
            // Registrar el broker en el registro RMI con la URL proporcionada
            Naming.rebind(args[0], broker);
            System.out.println("MessageBroker registered at: " + args[0]);
            System.out.println("Message broker started and ready to accept connections");
            
            // Las colas ya se crearon en el constructor, no es necesario crearlas aquí
            System.out.println("\nSystems available channels:");
            List<String> colas = broker.listarColas();
            for (int i = 0; i < colas.size(); i++) {
                System.out.println((i+1) + ". " + colas.get(i));
            }
            
        } catch (Exception e) {
            System.err.println("Exception in the MessageBroker: " + e.toString());
            e.printStackTrace();
        }
    }
}