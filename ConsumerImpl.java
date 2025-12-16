import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/**
 * Cliente consumidor que se conecta a un MessageBroker para recibir mensajes de una cola.
 */
public class ConsumerImpl extends UnicastRemoteObject implements Consumer, ConsumerCallback {
    // Ponerlo de forma implicita pq sino da warning y si cambiamos la clase puede fallar la deserializacion
    private static final long serialVersionUID = 1L;
    private final String consumerId;
    private String nombreCola; // Para almacenar el nombre de la cola de la que se están consumiendo mensajes
    private MessageBroker broker; // Referencia al broker para enviar ACKs
    private boolean autoAck = false; // Por defecto, ACK manual
    
    protected ConsumerImpl() throws RemoteException {
        super();
        this.consumerId = UUID.randomUUID().toString();
        this.nombreCola = "unknown"; // Valor por defecto
    }
    
    @Override
    public String getId() throws RemoteException {
        return consumerId;
    }
    
    /**
     * Activa o desactiva el modo de confirmación automática
     * @param autoAck true para confirmación automática
     */
    public void setAutoAck(boolean autoAck) {
        this.autoAck = autoAck;
        System.out.println("Confirmation mode: " + (autoAck ? "Automatic" : "Manual"));
    }
    
    /**
     * Establece la referencia al broker
     * @param broker Instancia del broker
     */
    public void setBroker(MessageBroker broker) {
        this.broker = broker;
    }
    
    /**
     * Método que implementa la callback para recibir mensajes
     */
    @Override
    public void onMessage(String mensaje) throws RemoteException {
        // Si es un mensaje de sistema, mostrar directamente
        if (mensaje.startsWith("SYSTEM||")) {
            System.out.println("\n==================================================");
            System.out.println("SYSTEM MESSAGE");
            System.out.println(mensaje.substring(8));
            System.out.println("==================================================\n");
            return;
        }
        
        // Extraer ID y contenido del mensaje
        String[] parts = mensaje.split("\\|\\|", 2);
        String mensajeId = parts[0];
        String contenido = parts.length > 1 ? parts[1] : "";
        
        System.out.println("\n==================================================");
        System.out.println("NEW MESSAGE CONSUMED");
        System.out.println("Consumer ID: " + consumerId);
        System.out.println("Channel: " + nombreCola);
        System.out.println("Message ID: " + mensajeId);
        System.out.println("Content: " + contenido);
        System.out.println("==================================================\n");
        
        // Si está en modo auto-ack, confirmar automáticamente
        if (autoAck && broker != null) {
            try {
                boolean acked = broker.confirmarMensaje(nombreCola, mensajeId, this);
                System.out.println("Message automatically confirmed: " + acked);
            } catch (Exception e) {
                System.err.println("Error confirming message: " + e.getMessage());
            }
        } else {
            // En modo manual, mostrar opciones
            System.out.println("To confirm this message, use: ack " + mensajeId);
            System.out.println("To reject this message, use: nack " + mensajeId);
        }
    }
    
    /**
     * Confirma un mensaje (ACK)
     * @param mensajeId ID del mensaje a confirmar
     */
    public void confirmarMensaje(String mensajeId) {
        if (broker == null) {
            System.out.println("No connection to the broker");
            return;
        }
        
        try {
            boolean result = broker.confirmarMensaje(nombreCola, mensajeId, this);
            if (result) {
                System.out.println("Message " + mensajeId + " successfully confirmed");
            } else {
                System.out.println("Could not confirm the message " + mensajeId);
            }
        } catch (Exception e) {
            System.err.println("Error confirming message: " + e.getMessage());
        }
    }
    
    /**
     * Rechaza un mensaje (NACK)
     * @param mensajeId ID del mensaje a rechazar
     */
    public void rechazarMensaje(String mensajeId) {
        if (broker == null) {
            System.out.println("No connection to the broker");
            return;
        }
        
        try {
            boolean result = broker.rechazarMensaje(nombreCola, mensajeId, this);
            if (result) {
                System.out.println("Message " + mensajeId + " successfully rejected");
            } else {
                System.out.println("Could not reject the message " + mensajeId);
            }
        } catch (Exception e) {
            System.err.println("Error rejecting message: " + e.getMessage());
        }
    }
    
    /**
     * Establece el nombre de la cola de la que se están consumiendo mensajes
     * @param nombreCola Nombre de la cola
     */
    public void setNombreCola(String nombreCola) {
        this.nombreCola = nombreCola;
    }
    
    /**
     * Método principal que inicia el consumidor.
     * @param args Argumentos de línea de comandos. Se espera la URL del MessageBroker y la URL del Consumer.
     */
    public static void main(String[] args) {
        try {
            // Verificar si se proporcionaron los dos argumentos necesarios
            if (args.length < 2) {
                System.err.println("Two parameters are required: the MessageBroker URL and the Consumer URL.");
                System.err.println("Example: java ConsumerImpl rmi://localhost/MessageBroker rmi://localhost/Consumer");
                System.exit(1);
            }
            
            String urlBroker = args[0];
            String urlConsumer = args[1];
            
            // Crear la instancia del consumidor y registrarla en RMI
            ConsumerImpl consumer = new ConsumerImpl();
            
            Naming.rebind(urlConsumer, consumer);
            System.out.println("Consumer registered at: " + urlConsumer);
            
            System.out.println("Connecting to broker at: " + urlBroker);
            System.out.println("Consumer ID: " + consumer.getId());
            
            // Obtener la referencia al broker remoto
            MessageBroker broker = (MessageBroker) Naming.lookup(urlBroker);
            consumer.setBroker(broker);
            
            // Configurar el modo de ACK
            Scanner scanner = new Scanner(System.in);
            System.out.print("Do you want to use automatic confirmation? (y/n): ");
            boolean autoAck = scanner.nextLine().trim().equalsIgnoreCase("y");
            consumer.setAutoAck(autoAck);
            
            // Configurar política de distribución fair
            System.out.print("Do you want to use fair (balanced) distribution? (y/n): ");
            boolean fairDispatch = scanner.nextLine().trim().equalsIgnoreCase("y");
            
            // Obtener la lista de canales disponibles
            List<String> canalesDisponibles = broker.listarColas();
            
            if (canalesDisponibles.isEmpty()) {
                System.out.println("No channels available on the broker.");
                System.exit(1);
            }
            
            // Mostrar los canales disponibles y permitir selección
            System.out.println("\nAvailable channels:");
            for (int i = 0; i < canalesDisponibles.size(); i++) {
                System.out.println((i + 1) + ". " + canalesDisponibles.get(i));
            }
            
            // Solicitar al usuario que seleccione un canal
            System.out.print("\nSelect one channel (1-" + canalesDisponibles.size() + "): ");
            int opcion = scanner.nextInt();
            scanner.nextLine(); // Consumir el salto de línea
            
            if (opcion < 1 || opcion > canalesDisponibles.size()) {
                System.out.println("Invalid option. Selecting the first channel by default.");
                opcion = 1;
            }
            
            String canalSeleccionado = canalesDisponibles.get(opcion - 1);
            consumer.setNombreCola(canalSeleccionado);
            
            System.out.println("Selected channel: " + canalSeleccionado);
            
            // Configurar el modo de distribución
            broker.setFairDispatch(canalSeleccionado, fairDispatch);
            System.out.println("Fair distribution: " + (fairDispatch ? "enabled" : "disabled"));
            
            // Suscribirse a la cola para recibir mensajes
            System.out.println("Subscribing to the channel '" + canalSeleccionado + "'...");
            broker.consumir(canalSeleccionado, consumer);
            
            System.out.println("Consumer started and waiting for messages from the channel '" + canalSeleccionado + "'...");
            System.out.println("When messages are consumed, they will be displayed automatically.");
            
            if (!autoAck) {
                System.out.println("\nAvailable commands:");
                System.out.println("  ack ID  - Confirm a message by its ID");
                System.out.println("  nack ID - Reject a message by its ID");
                System.out.println("  exit    - Exit the program");
            }
            
            // Creamos un hilo que periódicamente mostrará un mensaje de espera
            Thread monitorThread = new Thread(() -> {
                try {
                    while (true) {
                        // Esperar entre cada mensaje de estado
                        Thread.sleep(5000);
                        
                        // Mostrar que estamos esperando mensajes
                        System.out.println("Waiting for messages from the channel '" + canalSeleccionado + "'...");
                    }
                } catch (InterruptedException e) {
                    // Terminamos si se interrumpe
                }
            });
            // El programa termina automáticamente cuando todos los hilos principales (no demonios) han finalizado
            monitorThread.setDaemon(true); // Hilo demonio que termina con el principal
            monitorThread.start();
            
            // Agregar un shutdown hook para desuscribirse al cerrar
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("\nUnsubscribing consumer from the queue '" + canalSeleccionado + "'...");
                    broker.desuscribir(canalSeleccionado, consumer);
                    System.out.println("Consumer successfully unsubscribed.");
                } catch (Exception e) {
                    System.err.println("Error unsubscribing: " + e.getMessage());
                }
            }));
            
            // Bucle para procesar comandos manuales si no es autoAck
            if (!autoAck) {
                boolean ejecutando = true;
                while (ejecutando) {
                    String comando = scanner.nextLine().trim();
                    if (comando.equalsIgnoreCase("exit")) {
                        ejecutando = false;
                    } else if (comando.startsWith("ack ")) {
                        String mensajeId = comando.substring(4).trim();
                        consumer.confirmarMensaje(mensajeId);
                    } else if (comando.startsWith("nack ")) {
                        String mensajeId = comando.substring(5).trim();
                        consumer.rechazarMensaje(mensajeId);
                    } else if (!comando.isEmpty()) {
                        System.out.println("Command not recognized. Use 'ack ID', 'nack ID', or 'exit'");
                    }
                }
                
                System.out.println("Shutting down consumer...");
                broker.desuscribir(canalSeleccionado, consumer);
                System.exit(0);
            } else {
                // Mantener el programa en ejecución
                try { 
                    Thread.sleep(Long.MAX_VALUE); 
                } catch (InterruptedException e) { 
                    System.out.println("Consumer interrupted");
                }
            }
            
        } catch (Exception e) {
            System.out.println("Error in consumer: ");
            e.printStackTrace();
        }
    }
}