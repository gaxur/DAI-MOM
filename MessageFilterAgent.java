import java.io.Serializable;

/**
 * Interfaz para agentes que filtran mensajes.
 * Los agentes deciden si un mensaje debe ser entregado o descartado.
 */
public interface MessageFilterAgent extends Serializable {
    
    /**
     * Evalúa si un mensaje debe ser aceptado o rechazado
     * @param mensaje Contenido del mensaje a evaluar
     * @param nombreCola Nombre de la cola donde se publicó el mensaje
     * @return true si el mensaje debe ser aceptado, false si debe ser descartado
     */
    boolean aceptarMensaje(String mensaje, String nombreCola);
    
    /**
     * Retorna el nombre del agente
     * @return Nombre identificativo del agente
     */
    String getNombre();
    
    /**
     * Retorna una descripción de la lógica del agente
     * @return Descripción del criterio de filtrado
     */
    String getDescripcion();
    
    /**
     * Prioridad del agente (mayor número = mayor prioridad)
     * Los agentes se ejecutan en orden de prioridad
     * @return Prioridad del agente
     */
    default int getPrioridad() {
        return 0;
    }
}
