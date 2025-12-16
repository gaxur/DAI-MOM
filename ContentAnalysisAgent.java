import java.util.regex.Pattern;

/**
 * Agente que filtra mensajes basándose en análisis de contenido
 * Detecta contenido malicioso o inapropiado
 */
public class ContentAnalysisAgent implements MessageFilterAgent {
    // Ponerlo de forma implicita pq sino da warning y si cambiamos la clase puede fallar la deserializacion
    private static final long serialVersionUID = 1L;
    
    // Patrones regex para detectar contenido sospechoso
    private static final Pattern URL_SOSPECHOSA = Pattern.compile(
        "(http://|https://)?[a-z0-9-]+(\\.[a-z0-9-]+)*(\\.[a-z]{2,})(:[0-9]+)?(/.*)?",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CODIGO_MALICIOSO = Pattern.compile(
        "(eval\\(|exec\\(|<script|javascript:|onclick=|onerror=)",
        Pattern.CASE_INSENSITIVE
    );
    
    private final boolean bloquearUrls;
    private final boolean bloquearCodigo;
    
    /**
     * Constructor con opciones personalizadas
     * @param bloquearUrls Si debe bloquear mensajes con URLs
     * @param bloquearCodigo Si debe bloquear mensajes con código potencialmente malicioso
     */
    public ContentAnalysisAgent(boolean bloquearUrls, boolean bloquearCodigo) {
        this.bloquearUrls = bloquearUrls;
        this.bloquearCodigo = bloquearCodigo;
    }
    
    /**
     * Constructor con valores por defecto (solo bloquea código malicioso)
     */
    public ContentAnalysisAgent() {
        this(false, true);
    }
    
    @Override
    public boolean aceptarMensaje(String mensaje, String nombreCola) {
        // Verificar código malicioso
        if (bloquearCodigo && CODIGO_MALICIOSO.matcher(mensaje).find()) {
            System.out.println("[CONTENT ANALYSIS] Message rejected: contains potentially malicious code");
            return false;
        }
        
        // Verificar URLs si está habilitado
        if (bloquearUrls && URL_SOSPECHOSA.matcher(mensaje).find()) {
            System.out.println("[CONTENT ANALYSIS] Message rejected: contains disallowed URLs");
            return false;
        }
        
        // Verificar caracteres sospechosos en exceso
        long caracteresEspeciales = mensaje.chars()
            .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
            .count();
        
        if (caracteresEspeciales > mensaje.length() * 0.3) { // Más del 30% de caracteres especiales
            System.out.println("[CONTENT ANALYSIS] Message rejected: too many special characters");
            return false;
        }
        
        System.out.println("[CONTENT ANALYSIS] Message accepted");
        return true;
    }
    
    @Override
    public String getNombre() {
        return "ContentAnalysisAgent";
    }
    
    @Override
    public String getDescripcion() {
        return "Analyze the message content to detect malicious code or suspicious URLs";
    }
    
    @Override
    public int getPrioridad() {
        return 8; // Alta prioridad para seguridad
    }
}
