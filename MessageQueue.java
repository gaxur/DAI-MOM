import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.Instant;
import java.time.Duration;
import java.io.Serializable;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.File;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación de una cola de mensajes en el broker
 */
public class MessageQueue implements Serializable {
// Ponerlo de forma implicita pq sino da warning y si cambiamos la clase puede fallar la deserializacion
    private static final long serialVersionUID = 1L;
    
    // Mensaje con timestamp para control de expiración
    private static class MensajeTimestamp implements Serializable {
        private static final long serialVersionUID = 1L;
        
        String id;           // Identificador único para ACK
        String contenido;    // Contenido del mensaje
        Instant timestamp;   // Momento de creación
        boolean durable;     // Si el mensaje es durable
        boolean entregado;   // Si ya fue entregado a algún consumidor
        boolean acked;       // Si fue confirmado por el consumidor
        
        MensajeTimestamp(String contenido, boolean durable) {
            this.id = UUID.randomUUID().toString();
            this.contenido = contenido;
            this.timestamp = Instant.now();
            this.durable = durable;
            this.entregado = false;
            this.acked = false;
        }
        
        boolean estaExpirado() {
            // Expiración después de 5 minutos (300 segundos)
            return Instant.now().isAfter(timestamp.plusSeconds(300));
        }
        
        long tiempoRestante() {
            Instant limite = timestamp.plusSeconds(300);
            Duration duracion = Duration.between(Instant.now(), limite);
            return Math.max(0, duracion.getSeconds());
        }
    }
    
    private String nombre;   // Nombre de la cola
    private boolean durable; // Si la cola es durable
    private final ConcurrentLinkedQueue<MensajeTimestamp> mensajes = new ConcurrentLinkedQueue<>(); // FIFO
    private final CopyOnWriteArrayList<ConsumerCallback> consumidores = new CopyOnWriteArrayList<>();
    private final Map<String, MensajeTimestamp> mensajesNoConfirmados = new ConcurrentHashMap<>();
    // Rond robin --> igualdad de distribución entre consumidores
    // Fair dispatch --> un consumidor no recibe otro mensaje hasta confirmar el anterior (optimo)
    private int currentConsumerIndex = 0; // Índice para round robin
    private boolean fairDispatch = true;  // Por defecto, activamos fair dispatch
    
    /**
     * Constructor básico
     */
    public MessageQueue() {
        this.durable = false;
        this.nombre = "queue_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Constructor con nombre y durabilidad
     */
    public MessageQueue(String nombre, boolean durable) {
        this.nombre = nombre;
        this.durable = durable;
        
        // Si la cola es durable, tratar de recuperar mensajes
        if (durable) {
            recuperarMensajesPersistentes();
        }
    }
    
    /**
     * Establece si la cola usa política de fair dispatch
     * @param fairDispatch true para activar fair dispatch
     */
    public void setFairDispatch(boolean fairDispatch) {
        this.fairDispatch = fairDispatch;
        System.out.println("Queue '" + nombre + "': Fair dispatch " + 
                          (fairDispatch ? "enabled" : "disabled"));
    }
    
    /**
     * Devuelve el nombre de la cola
     * @return Nombre de la cola
     */
    public String getNombre() {
        return nombre;
    }
    
    /**
     * Indica si la cola es durable
     * @return true si es durable
     */
    public boolean esDurable() {
        return durable;
    }
    
    /**
     * Publica un mensaje en la cola
     * @param mensaje Contenido del mensaje
     */
    public void publicar(String mensaje) {
        publicar(mensaje, this.durable);
    }
    
    /**
     * Publica un mensaje en la cola con opción de durabilidad
     * @param mensaje Contenido del mensaje
     * @param durable Si el mensaje debe persistir
     */
    public void publicar(String mensaje, boolean durable) {
        MensajeTimestamp mensajeTS = new MensajeTimestamp(mensaje, durable);
        
        // Si hay consumidores disponibles, enviar directamente
        if (!consumidores.isEmpty()) {
            enviarMensajeAConsumidor(mensajeTS);
        } else {
            // Si no hay consumidores, guardar en la cola
            mensajes.add(mensajeTS);
            System.out.println("Message stored in queue '" + nombre + 
                             "'. It will be deleted in 5 minutes if there is no consumer.");
            
            // Si es durable, persistir cambios
            if (durable && this.durable) {
                persistirMensajes();
            }
        }
    }
    
    /**
     * Envía un mensaje a un consumidor usando round robin
     * @param mensaje Mensaje a enviar
     */
    private void enviarMensajeAConsumidor(MensajeTimestamp mensaje) {
        if (consumidores.isEmpty()) {
            // Si no hay consumidores, guardar en la cola
            mensajes.add(mensaje);
            return;
        }
        
        // Garantizar que el índice esté dentro del rango
        synchronized (this) {
            if (currentConsumerIndex >= consumidores.size()) {
                currentConsumerIndex = 0;
            }
            
            ConsumerCallback consumer = consumidores.get(currentConsumerIndex);
            // Guardar mensaje en mapa de no confirmados
            mensajesNoConfirmados.put(mensaje.id, mensaje);
            mensaje.entregado = true;
            
            // Para fair dispatch, solo incrementamos después de ACK
            if (!fairDispatch) {
                // Si no es fair dispatch, round robin inmediato
                currentConsumerIndex = (currentConsumerIndex + 1) % consumidores.size();
            }
            
            // Enviar mensaje al consumidor en un hilo separado
            new Thread(() -> {
                try {
                    int consumerNum = currentConsumerIndex;
                    // Enviar ID junto con el mensaje para ACK
                    String mensajeConId = mensaje.id + "||" + mensaje.contenido;
                    consumer.onMessage(mensajeConId);
                    System.out.println("Message delivered to consumer #" + consumerNum + 
                                     " using distribution " + (fairDispatch ? "fair" : "round robin") + ".");
                } catch (RemoteException e) {
                    System.err.println("Error notifying the consumer: " + e.getMessage());
                    
                    // Remover consumidor si no está disponible
                    try {
                        consumidores.remove(consumer);
                        System.out.println("Consumer removed due to communication error.");
                    } catch (Exception ex) {
                        // Ignorar errores al remover
                    }
                    
                    // Si hay error, intentar con otro consumidor o guardar de nuevo
                    if (!consumidores.isEmpty()) {
                        // Intenta con el siguiente consumidor
                        enviarMensajeAConsumidor(mensaje);
                    } else {
                        // Devolver a la cola
                        mensajesNoConfirmados.remove(mensaje.id);
                        mensaje.entregado = false;
                        mensajes.add(mensaje);
                        System.out.println("Message returned to the queue because no consumers are available.");
                    }
                }
            }).start();
        }
    }
    
    /**
     * Confirma un mensaje como procesado (ACK)
     * @param mensajeId Identificador del mensaje
     * @param consumidor Consumidor que confirma
     * @return true si el mensaje fue confirmado
     */
    public boolean confirmarMensaje(String mensajeId, ConsumerCallback consumidor) {
        MensajeTimestamp mensaje = mensajesNoConfirmados.remove(mensajeId);
        if (mensaje != null) {
            mensaje.acked = true;
            System.out.println("Message " + mensajeId + " acknowledged (ACK) by consumer.");
            
            // En fair dispatch, solo avanzamos round robin cuando hay ACK
            if (fairDispatch) {
                synchronized(this) {
                    currentConsumerIndex = (currentConsumerIndex + 1) % consumidores.size();
                }
            }
            
            // Si es durable, actualizar estado persistente
            if (mensaje.durable && this.durable) {
                persistirMensajes();
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Rechaza un mensaje, devolviéndolo a la cola
     * @param mensajeId Identificador del mensaje
     * @param consumidor Consumidor que rechaza
     * @return true si el mensaje fue rechazado y devuelto a la cola
     */
    public boolean rechazarMensaje(String mensajeId, ConsumerCallback consumidor) {
        MensajeTimestamp mensaje = mensajesNoConfirmados.remove(mensajeId);
        if (mensaje != null) {
            mensaje.entregado = false;
            mensajes.add(mensaje);
            System.out.println("Message " + mensajeId + " rejected (NACK) by consumer and returned to the queue.");
            
            // En fair dispatch, avanzamos aún con NACK para evitar bloqueos
            if (fairDispatch) {
                synchronized(this) {
                    currentConsumerIndex = (currentConsumerIndex + 1) % consumidores.size();
                }
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Registra un consumidor para recibir mensajes
     * @param callback Función de procesamiento de mensajes
     */
    public void registrarConsumidor(ConsumerCallback callback) {
        consumidores.add(callback);
        int totalConsumidores = consumidores.size();
        System.out.println("New consumer registered in the queue '" + nombre + 
                         "'. Total consumers: " + totalConsumidores);
        System.out.println("Messages will be distributed using " + 
                         (fairDispatch ? "fair dispatch" : "round robin") + 
                         " among " + totalConsumidores + " consumers.");
        
        // Intentar procesar mensajes pendientes inmediatamente
        procesarMensajesPendientes();
    }
    
    /**
     * Desuscribe un consumidor de la cola
     * @param callback Consumidor a desuscribir
     * @return true si se desuscribió correctamente
     */
    public boolean desuscribirConsumidor(ConsumerCallback callback) {
        boolean removed = consumidores.remove(callback);
        if (removed) {
            int totalConsumidores = consumidores.size();
            System.out.println("Consumer unsubscribed from the queue '" + nombre + 
                             "'. Total consumers: " + totalConsumidores);
            
            // Ajustar el índice si es necesario
            synchronized (this) {
                if (currentConsumerIndex >= consumidores.size() && consumidores.size() > 0) {
                    currentConsumerIndex = 0;
                }
            }
        }
        return removed;
    }
    
    /**
     * Procesa los mensajes pendientes en la cola
     */
    private void procesarMensajesPendientes() {
        // Procesar mientras haya mensajes y consumidores
        while (!mensajes.isEmpty() && !consumidores.isEmpty()) {
            MensajeTimestamp mensaje = mensajes.poll();
            if (mensaje == null) break;
            
            if (mensaje.estaExpirado()) {
                System.out.println("Expired message discarded: " + mensaje.contenido);
                continue;
            }
            
            // Enviar mensaje a un consumidor usando round robin
            enviarMensajeAConsumidor(mensaje);
        }
    }
    
    /**
     * Elimina mensajes expirados de la cola
     */
    public void eliminarMensajesExpirados() {
        int contadorEliminados = 0;
        ConcurrentLinkedQueue<MensajeTimestamp> mensajesValidos = new ConcurrentLinkedQueue<>();
        
        for (MensajeTimestamp mensaje : mensajes) {
            if (!mensaje.estaExpirado()) {
                mensajesValidos.add(mensaje);
                System.out.println("Message pending in the queue. '" + nombre + 
                                 "'. Time remaining: " + mensaje.tiempoRestante() + " seconds.");
            } else {
                contadorEliminados++;
                System.out.println("Expired message removed from the queue. '" + nombre + "': " + mensaje.contenido);
            }
        }
        
        // Reemplazar la cola con solo los mensajes válidos
        mensajes.clear();
        mensajes.addAll(mensajesValidos);
        
        if (contadorEliminados > 0) {
            System.out.println("They were deleted " + contadorEliminados + " expired messages from the queue '" + nombre + "'.");
            
            // Si es durable, persistir cambios después de limpieza
            if (this.durable) {
                persistirMensajes();
            }
        }
    }
    
    /**
     * Persiste los mensajes durables en disco
     */
    private void persistirMensajes() {
        if (!durable) return;
        
        String filename = "queue_" + nombre + ".dat";
        try (FileOutputStream fos = new FileOutputStream(filename);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            
            // Guardar solo los mensajes durables que no han sido acked
            ConcurrentLinkedQueue<MensajeTimestamp> mensajesDurables = new ConcurrentLinkedQueue<>();
            for (MensajeTimestamp msg : mensajes) {
                if (msg.durable && !msg.acked) {
                    mensajesDurables.add(msg);
                }
            }
            
            // También guardar los mensajes no confirmados
            for (MensajeTimestamp msg : mensajesNoConfirmados.values()) {
                if (msg.durable && !msg.acked) {
                    mensajesDurables.add(msg);
                }
            }
            
            oos.writeObject(mensajesDurables);
            System.out.println("Persisted " + mensajesDurables.size() + 
                             " durable messages for the queue '" + nombre + "'");
        } catch (Exception e) {
            System.err.println("Error persisting messages: " + e.getMessage());
        }
    }
    
    /**
     * Recupera mensajes durables de disco
     */
    @SuppressWarnings("unchecked")
    private void recuperarMensajesPersistentes() {
        String filename = "queue_" + nombre + ".dat";
        File file = new File(filename);
        
        if (!file.exists()) {
            System.out.println("There are no persistence files for the queue '" + nombre + "'");
            return;
        }
        
        try (FileInputStream fis = new FileInputStream(filename);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            
            ConcurrentLinkedQueue<MensajeTimestamp> mensajesRecuperados = 
                (ConcurrentLinkedQueue<MensajeTimestamp>) ois.readObject();
            
            // Agregar los mensajes recuperados a la cola
            int count = 0;
            for (MensajeTimestamp msg : mensajesRecuperados) {
                if (!msg.estaExpirado()) {
                    mensajes.add(msg);
                    count++;
                }
            }
            
            System.out.println("Retrieved " + count + " durable messages for the queue '" + nombre + "'");
        } catch (Exception e) {
            System.err.println("Error retrieving persistent messages: " + e.getMessage());
        }
    }
    
    /**
     * Elimina la cola y sus recursos asociados
     */
    public void eliminar() {
        // Limpiar mensajes y consumidores
        mensajes.clear();
        mensajesNoConfirmados.clear();
        
        // Notificar a los consumidores que la cola se está eliminando
        for (ConsumerCallback consumer : consumidores) {
            try {
                consumer.onMessage("SYSTEM||The queue '" + nombre + "' has been deleted.");
            } catch (RemoteException e) {
                // Ignorar errores al notificar
            }
        }
        consumidores.clear();
        
        // Eliminar archivo de persistencia si existe
        if (durable) {
            String filename = "queue_" + nombre + ".dat";
            File file = new File(filename);
            if (file.exists()) {
                file.delete();
                System.out.println("Persistence file deleted for the queue '" + nombre + "'");
            }
        }
        
        System.out.println("Queue '" + nombre + "' deleted successfully.");
    }
    
    /**
     * Retorna el número de mensajes en la cola
     * @return Número de mensajes
     */
    public int contarMensajes() {
        return mensajes.size() + mensajesNoConfirmados.size();
    }
    
    /**
     * Retorna el número de consumidores suscritos
     * @return Número de consumidores
     */
    public int contarConsumidores() {
        return consumidores.size();
    }
}