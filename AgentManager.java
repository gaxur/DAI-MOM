import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gestor de agentes de filtrado de mensajes
 * Coordina la ejecución de múltiples agentes IA
 */
public class AgentManager {
    // Cada vez que se modifica la lista de agentes, se crea una nueva copia (thread-safe)
    private final CopyOnWriteArrayList<MessageFilterAgent> agentes = new CopyOnWriteArrayList<>();
    private boolean habilitado = true;
    
    /**
     * Constructor por defecto - inicializa sin agentes
     */
    public AgentManager() {
        // Inicializar sin agentes por defecto
    }
    
    /**
     * Agrega un agente al sistema
     * @param agente Agente a agregar
     */
    public void agregarAgente(MessageFilterAgent agente) {
        agentes.add(agente);
        // Ordenar por prioridad (mayor prioridad primero)
        agentes.sort((a1, a2) -> Integer.compare(a2.getPrioridad(), a1.getPrioridad()));
        System.out.println(" Attached agent: " + agente.getNombre() + 
                         " (Priority: " + agente.getPrioridad() + ")");
        System.out.println(" Description: " + agente.getDescripcion());
    }
    
    /**
     * Elimina un agente del sistema
     * @param nombreAgente Nombre del agente a eliminar
     * @return true si se eliminó correctamente
     */
    public boolean eliminarAgente(String nombreAgente) {
        return agentes.removeIf(agente -> agente.getNombre().equals(nombreAgente));
    }
    
    /**
     * Evalúa un mensaje con todos los agentes
     * Todos los agentes deben aceptar el mensaje para que sea válido
     * @param mensaje Contenido del mensaje
     * @param nombreCola Nombre de la cola
     * @return true si todos los agentes aceptan el mensaje
     */
    public boolean evaluarMensaje(String mensaje, String nombreCola) {
        if (!habilitado) {
            return true; // Si está deshabilitado, aceptar todos
        }
        
        if (agentes.isEmpty()) {
            System.out.println(" No agents configured, accepting default message");
            return true;
        }
        
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("AGENT EVALUATION");
        System.out.println("   Queue: " + nombreCola);
        System.out.println("   Message: " + (mensaje.length() > 50 ? 
                         mensaje.substring(0, 47) + "..." : mensaje));
        System.out.println("   Active agents: " + agentes.size());
        System.out.println("───────────────────────────────────────────────────────");
        
        // Todos los agentes deben aceptar el mensaje
        for (MessageFilterAgent agente : agentes) {
            try {
                boolean aceptado = agente.aceptarMensaje(mensaje, nombreCola);
                if (!aceptado) {
                    System.out.println("───────────────────────────────────────────────────────");
                    System.out.println("MESSAGE REJECTED by " + agente.getNombre());
                    System.out.println("═══════════════════════════════════════════════════════\n");
                    return false;
                }
            } catch (Exception e) {
                System.err.println(" Error in agent " + agente.getNombre() + ": " + e.getMessage());
                // Si hay error, ser conservador y rechazar
                System.out.println("═══════════════════════════════════════════════════════\n");
                return false;
            }
        }
        
        System.out.println("───────────────────────────────────────────────────────");
        System.out.println("MESSAGE ACCEPTED by all agents");
        System.out.println("═══════════════════════════════════════════════════════\n");
        return true;
    }
    
    /**
     * Lista todos los agentes activos
     * @return Lista de agentes
     */
    public List<MessageFilterAgent> listarAgentes() {
        return new ArrayList<>(agentes);
    }
    
    /**
     * Habilita o deshabilita el sistema de agentes
     * @param habilitado true para habilitar, false para deshabilitar
     */
    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
        System.out.println("Agent system: " + (habilitado ? "ENABLED" : "DISABLED"));
    }
    
    /**
     * Verifica si el sistema de agentes está habilitado
     * @return true si está habilitado
     */
    public boolean isHabilitado() {
        return habilitado;
    }
    
    /**
     * Obtiene el número de agentes activos
     * @return Número de agentes
     */
    public int contarAgentes() {
        return agentes.size();
    }
    
    /**
     * Configura agentes por defecto para el sistema
     */
    public void configurarAgentesPorDefecto() {
        System.out.println("\nConfiguring default agents...");
        agregarAgente(new SpamFilterAgent());
        agregarAgente(new ContentAnalysisAgent());
        agregarAgente(new LengthFilterAgent());
        agregarAgente(new ChannelRulesAgent());
        System.out.println("Agent configuration completed\n");
    }
}
