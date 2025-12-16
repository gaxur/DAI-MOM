import java.util.Arrays;
import java.util.List;

/**
 * Agente que filtra mensajes tipo spam basándose en palabras clave sospechosas
 */
public class SpamFilterAgent implements MessageFilterAgent {
    // Ponerlo de forma implicita pq sino da warning y si cambiamos la clase puede fallar la deserializacion        
    private static final long serialVersionUID = 1L;
    
    // Lista de palabras clave que identifican spam
    private final List<String> palabrasClaveSospechas = Arrays.asList(
        "spam", "phishing", "malware", "virus", "hack", 
        "free reward", "won", "claim now", 
        "urgent", "click here", "limited offer"
    );
    
    @Override
    public boolean aceptarMensaje(String mensaje, String nombreCola) {
        String mensajeLower = mensaje.toLowerCase();
        
        // Rechazar si contiene palabras sospechosas
        for (String palabra : palabrasClaveSospechas) {
            if (mensajeLower.contains(palabra.toLowerCase())) {
                System.out.println("[SPAM FILTER] Message rejected for containing: '" + palabra + "'");
                return false;
            }
        }
        
        System.out.println("[SPAM FILTER] Accepted message: " + mensaje);
        return true;
    }
    
    @Override
    public String getNombre() {
        return "SpamFilterAgent";
    }
    
    @Override
    public String getDescripcion() {
        return "Filters messages containing keywords related to spam";
    }
    
    @Override
    public int getPrioridad() {
        return 10; // Alta prioridad para filtrar spam primero
    }
}
