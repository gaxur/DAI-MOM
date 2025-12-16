import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Scanner;

/**
 * Cliente productor que se conecta a un MessageBroker para enviar mensajes a una cola.
 * También se registra en RMI para poder ser contactado.
 */
public class ProducerImpl extends UnicastRemoteObject implements Producer {
    // Ponerlo de forma implicita pq sino da warning y si cambiamos la clase puede fallar la deserializacion    
    private static final long serialVersionUID = 1L;
    
    protected ProducerImpl() throws RemoteException {
        super(); // Llamada al constructor de UnicastRemoteObject
    }
    
    @Override
    public String getName() throws RemoteException {
        return "Producer";
    }
    
    public static void main(String[] args) {
        try {
            // Verificar si se proporcionaron los dos argumentos necesarios
            if (args.length < 2) {
                System.err.println("Two parameters are required: the MessageBroker URL and the Producer URL.");
                System.err.println("Example: java ProducerImpl rmi://localhost/MessageBroker rmi://localhost/Producer");
                System.exit(1);
            }
            
            String urlBroker = args[0];
            String urlProducer = args[1];
            
            // Crear la instancia del productor y registrarla en RMI
            ProducerImpl producer = new ProducerImpl();
            Naming.rebind(urlProducer, producer);
            System.out.println("Producer registered at: " + urlProducer);
            
            System.out.println("Connecting to broker at: " + urlBroker);
            
            // Obtener la referencia al broker remoto
            MessageBroker broker = (MessageBroker) Naming.lookup(urlBroker);
            
            // Bucle para permitir al usuario enviar múltiples mensajes
            Scanner scanner = new Scanner(System.in);
            boolean continuar = true;
            
            while (continuar) {
                System.out.println("\nAVAILABLE COMMANDS:");
                System.out.println("1. Send message");
                System.out.println("2. List queues");
                System.out.println("3. Create new queue");
                System.out.println("4. Eliminate queue");
                System.out.println("5. View queue information");
                System.out.println("0. Exit");
                System.out.print("\nSelect one option: ");
                
                int opcion = scanner.nextInt();
                scanner.nextLine(); // Consumir el salto de línea
                
                switch (opcion) {
                    case 0:
                        continuar = false;
                        System.out.println("Shutting down producer...");
                        break;
                        
                    case 1: // Enviar mensaje
                        // Obtener la lista de canales disponibles
                        List<String> canalesDisponibles = broker.listarColas();
                        
                        if (canalesDisponibles.isEmpty()) {
                            System.out.println("No channels available in the broker.");
                            continue;
                        }
                        
                        // Mostrar los canales disponibles y permitir selección
                        System.out.println("\nAvailable channels:");
                        for (int i = 0; i < canalesDisponibles.size(); i++) {
                            System.out.println((i + 1) + ". " + canalesDisponibles.get(i));
                        }
                        
                        System.out.print("\nSelect a channel (1-" + canalesDisponibles.size() + "): ");
                        int canalNum = scanner.nextInt();
                        scanner.nextLine(); // Consumir el salto de línea
                        
                        if (canalNum < 1 || canalNum > canalesDisponibles.size()) {
                            System.out.println("Invalid option. Please try again.");
                            continue;
                        }
                        
                        String canalSeleccionado = canalesDisponibles.get(canalNum - 1);
                        
                        // Preguntar si quiere que el mensaje sea durable
                        System.out.print("Do you want this message to be durable? (y/n): ");
                        boolean mensajeDurable = scanner.nextLine().trim().equalsIgnoreCase("y");
                        
                        // Permitir al usuario ingresar el mensaje
                        System.out.print("Enter the message to send to channel '" + canalSeleccionado + "': ");
                        String mensaje = scanner.nextLine();
                        
                        if (mensaje.trim().isEmpty()) {
                            mensaje = "Empty Message";
                        }
                        
                        // Publicar el mensaje
                        System.out.println("Publishing message to channel '" + canalSeleccionado + 
                                         "' (durable: " + mensajeDurable + "): " + mensaje);
                        broker.publicar(canalSeleccionado, mensaje, mensajeDurable);
                        System.out.println("Message successfully sent to channel '" + canalSeleccionado + "'");
                        break;
                        
                    case 2: // Listar colas
                        List<String> colas = broker.listarColas();
                        System.out.println("\nAvailable queues in the broker:");
                        if (colas.isEmpty()) {
                            System.out.println("No queues defined.");
                        } else {
                            for (int i = 0; i < colas.size(); i++) {
                                System.out.println((i + 1) + ". " + colas.get(i));
                            }
                        }
                        break;
                        
                    case 3: // Crear nueva cola
                        System.out.print("Enter the name of the new queue: ");
                        String nombreCola = scanner.nextLine().trim();
                        
                        if (nombreCola.isEmpty()) {
                            System.out.println("Queue name cannot be empty.");
                            continue;
                        }
                        
                        System.out.print("Do you want the queue to be durable? (y/n): ");
                        boolean colaDurable = scanner.nextLine().trim().equalsIgnoreCase("y");
                        
                        broker.declararCola(nombreCola, colaDurable);
                        System.out.println("Queue '" + nombreCola + "' created successfully (durable: " + 
                                         colaDurable + ")");
                        break;
                        
                    case 4: // Eliminar cola
                        List<String> colasEliminar = broker.listarColas();
                        System.out.println("\nAvailable queues for deletion:");
                        if (colasEliminar.isEmpty()) {
                            System.out.println("No queues defined.");
                            continue;
                        }
                        
                        for (int i = 0; i < colasEliminar.size(); i++) {
                            System.out.println((i + 1) + ". " + colasEliminar.get(i));
                        }
                        
                        System.out.print("\nSelect the queue to delete (1-" + colasEliminar.size() + "): ");
                        int colaNum = scanner.nextInt();
                        scanner.nextLine(); // Consumir el salto de línea
                        
                        if (colaNum < 1 || colaNum > colasEliminar.size()) {
                            System.out.println("Invalid option. Please try again.");
                            continue;
                        }
                        
                        String colaEliminar = colasEliminar.get(colaNum - 1);
                        boolean eliminada = broker.eliminarCola(colaEliminar);
                        
                        if (eliminada) {
                            System.out.println("Queue '" + colaEliminar + "' deleted successfully.");
                        } else {
                            System.out.println("It could not be deleted the queue '" + colaEliminar + "'.");
                        }
                        break;
                        
                    case 5: // Ver información de cola
                        List<String> colasInfo = broker.listarColas();
                        System.out.println("\nAvailable queues:");
                        if (colasInfo.isEmpty()) {
                            System.out.println("No queues defined.");
                            continue;
                        }
                        
                        for (int i = 0; i < colasInfo.size(); i++) {
                            System.out.println((i + 1) + ". " + colasInfo.get(i));
                        }
                        
                        System.out.print("\nSelect the queue to view information (1-" + colasInfo.size() + "): ");
                        int infoNum = scanner.nextInt();
                        scanner.nextLine(); // Consumir el salto de línea
                        
                        if (infoNum < 1 || infoNum > colasInfo.size()) {
                            System.out.println("Invalid option. Please try again.");
                            continue;
                        }
                        
                        String colaInfo = colasInfo.get(infoNum - 1);
                        String info = broker.getInfoCola(colaInfo);
                        System.out.println("\nQUEUE INFORMATION:");
                        System.out.println(info);
                        break;
                        
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
            
            scanner.close();
            
        } catch (Exception e) {
            System.out.println("Producer Error: ");
            e.printStackTrace();
        }
    }
}