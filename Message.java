import java.time.Instant;

/**
 * Representa un mensaje individual con su timestamp de creación.
 */
public class Message {
    final String contenido;
    final Instant timestamp;

    public Message(String contenido) {
        this.contenido = contenido;
        this.timestamp = Instant.now();
    }

    /**
     * Determina si el mensaje ha expirado
     * @param ahora Tiempo de referencia
     * @param minutosExpiracion Límite en minutos
     * @return true si el mensaje ha expirado
     */
    public boolean estaExpirado(Instant ahora, int minutosExpiracion) {
        return ahora.isAfter(timestamp.plusSeconds(minutosExpiracion * 60L));
    }
}