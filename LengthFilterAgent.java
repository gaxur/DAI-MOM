/**
 * Agente que filtra mensajes basándose en su longitud
 */
public class LengthFilterAgent implements MessageFilterAgent {
    private static final long serialVersionUID = 1L;
    
    private final int longitudMinima;
    private final int longitudMaxima;
    
    /**
     * Constructor con límites personalizados
     * @param longitudMinima Longitud mínima aceptable
     * @param longitudMaxima Longitud máxima aceptable
     */
    public LengthFilterAgent(int longitudMinima, int longitudMaxima) {
        this.longitudMinima = longitudMinima;
        this.longitudMaxima = longitudMaxima;
    }
    
    /**
     * Constructor con valores por defecto
     */
    public LengthFilterAgent() {
        this(5, 500); // Por defecto: entre 5 y 500 caracteres
    }
    
    @Override
    public boolean aceptarMensaje(String mensaje, String nombreCola) {
        int longitud = mensaje.length();
        
        if (longitud < longitudMinima) {
            System.out.println("[LENGTH FILTER] Message rejected: too short (" + 
                             longitud + " < " + longitudMinima + ")");
            return false;
        }
        
        if (longitud > longitudMaxima) {
            System.out.println("[LENGTH FILTER] Message rejected: too long (" + 
                             longitud + " > " + longitudMaxima + ")");
            return false;
        }
        
        System.out.println("[LENGTH FILTER] Message accepted (length: " + longitud + ")");
        return true;
    }
    
    @Override
    public String getNombre() {
        return "LengthFilterAgent";
    }
    
    @Override
    public String getDescripcion() {
        return "Filters messages based on their length (min: " + longitudMinima + 
               ", max: " + longitudMaxima + ")";
    }
    
    @Override
    public int getPrioridad() {
        return 5; // Prioridad media
    }
}
