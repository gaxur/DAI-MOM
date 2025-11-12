import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Agente que filtra mensajes basándose en reglas específicas por canal
 */
public class ChannelRulesAgent implements MessageFilterAgent {
    private static final long serialVersionUID = 1L;
    
    // Reglas específicas por canal
    private final Map<String, Pattern> reglasCanal = new HashMap<>();
    
    public ChannelRulesAgent() {
        // Configurar reglas por defecto para cada canal
        
        // Solo mensajes de notificaciones del sistema
        reglasCanal.put("canal_NOTIFICACION", Pattern.compile(
            "^\\[NOTIFICACION\\].*", Pattern.CASE_INSENSITIVE
        ));
        
        // Solo mensajes de alertas
        reglasCanal.put("canal_ALERTA", Pattern.compile(
            "^\\[ALERTA\\].*", Pattern.CASE_INSENSITIVE
        ));
        
        // Solo mensajes de información
        reglasCanal.put("canal_INFO", Pattern.compile(
            "^\\[INFO\\].*", Pattern.CASE_INSENSITIVE
        ));
        
        // Cualquier mensaje válido (sin restricciones) para el canal_GENERAL
    }
    
    /**
     * Agrega una regla personalizada para un canal
     * @param nombreCola Nombre del canal
     * @param patron Patrón regex que debe cumplir el mensaje
     */
    public void agregarRegla(String nombreCola, Pattern patron) {
        reglasCanal.put(nombreCola, patron);
    }
    
    @Override
    public boolean aceptarMensaje(String mensaje, String nombreCola) {
        // Si no hay regla específica para este canal, aceptar
        if (!reglasCanal.containsKey(nombreCola)) {
            System.out.println("[CHANNEL RULES] Message accepted (no specific rules for '" + 
                             nombreCola + "')");
            return true;
        }
        
        Pattern patron = reglasCanal.get(nombreCola);
        boolean cumpleRegla = patron.matcher(mensaje).matches();
        
        if (cumpleRegla) {
            System.out.println("[CHANNEL RULES] Message accepted for '" + nombreCola + "'");
        } else {
            System.out.println("[CHANNEL RULES] Message rejected: does not comply with the rules of '" + 
                             nombreCola + "'");
        }
        
        return cumpleRegla;
    }
    
    @Override
    public String getNombre() {
        return "ChannelRulesAgent";
    }
    
    @Override
    public String getDescripcion() {
        return "Validates that messages comply with the specific rules of each channel";
    }
    
    @Override
    public int getPrioridad() {
        return 7; // Prioridad alta-media
    }
}
