import java.rmi.Naming;

/**
 * Productor de prueba para demostrar el correcto funcionamiento del sistema de agentes de filtrado
 */
public class TestAgentsProducer {
    
    public static void main(String[] args) {
        try {
            String url = args.length > 0 ? args[0] : "rmi://localhost/MessageBroker";
            MessageBroker broker = (MessageBroker) Naming.lookup(url);
            
            System.out.println("═══════════════════════════════════════════════════════");
            System.out.println("AI AGENTS SYSTEM TEST");
            System.out.println("═══════════════════════════════════════════════════════\n");
            
            // Mostrar agentes activos
            System.out.println("Active agents in the system:");
            for (MessageFilterAgent agente : broker.listarAgentes()) {
                System.out.println("  • " + agente.getNombre() + " (Priority: " + 
                                 agente.getPrioridad() + ")");
                System.out.println("    " + agente.getDescripcion());
            }
            System.out.println();
            
            // Prueba 1: Mensaje válido para canal_NOTIFICACION
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 1: Valid message for canal_NOTIFICACION");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            boolean resultado1 = broker.publicar("canal_NOTIFICACION", "[NOTIFICACION] The system has been successfully updated");
            System.out.println("Result: " + (resultado1 ? "ACCEPTED" : "REJECTED"));
            
            // Prueba 2: Mensaje inválido para canal_NOTIFICACION (falta [NOTIFICACION])
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 2: Message without correct format for canal_NOTIFICACION");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            boolean resultado2 = broker.publicar("canal_NOTIFICACION", "This is a normal message without format");
            System.out.println("Result: " + (resultado2 ? "ACCEPTED" : "REJECTED"));
            
            // Prueba 3: Mensaje con spam
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 3: Message with spam");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            boolean resultado3 = broker.publicar("canal_GENERAL", "[INFO] You won a free prize, claim it now!");
            System.out.println("Result: " + (resultado3 ? "ACCEPTED" : "REJECTED"));
            
            // Prueba 4: Mensaje válido para canal_ALERTA
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 4: Valid message for canal_ALERTA");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            boolean resultado4 = broker.publicar("canal_ALERTA", "[ALERT] High CPU usage detected on the server");
            System.out.println("Result: " + (resultado4 ? "ACCEPTED" : "REJECTED"));
            
            // Prueba 5: Mensaje demasiado corto
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 5: Message too short");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            boolean resultado5 = broker.publicar("canal_GENERAL", "Hello");
            System.out.println("Result: " + (resultado5 ? "ACCEPTED" : "REJECTED"));
            
            // Prueba 6: Mensaje con código malicioso
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 6: Message with potentially malicious code");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            boolean resultado6 = broker.publicar("canal_GENERAL", "[INFO] Execute: <script>alert('xss')</script>");
            System.out.println("Result: " + (resultado6 ? "ACCEPTED" : "REJECTED"));
            
            // Prueba 7: Mensaje válido para canal_INFO (requiere [INFO])
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 7: Valid message for canal_INFO");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            boolean resultado7 = broker.publicar("canal_INFO", "[INFO] New user registered in the system");
            System.out.println("Result: " + (resultado7 ? "ACCEPTED" : "REJECTED"));
            
            // Prueba 8: Mensaje válido para canal_GENERAL (sin restricciones específicas)
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 8: Normal message for canal_GENERAL");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            boolean resultado8 = broker.publicar("canal_GENERAL", "This is a completely normal and safe message");
            System.out.println("Result: " + (resultado8 ? "ACCEPTED" : "REJECTED"));
            
            // Resumen de resultados
            int aceptados = 0;
            int rechazados = 0;
            boolean[] resultados = {resultado1, resultado2, resultado3, resultado4, 
                                   resultado5, resultado6, resultado7, resultado8};

            for (boolean r : resultados) {
                if (r) aceptados++; else rechazados++;
            }

            System.out.println("═══════════════════════════════════════════════════════\n");
            System.out.println("AGENTS SYSTEM TEST SUMMARY");
            System.out.println("═══════════════════════════════════════════════════════\n");
            System.out.println("Accepted messages: " + aceptados);
            System.out.println("Rejected messages: " + rechazados);
            System.out.println("Total tests: " + resultados.length);
            System.out.println("═══════════════════════════════════════════════════════\n");
            
            // Probar deshabilitar agentes
            System.out.println("\nDisabling agents system ...");
            broker.setAgentesHabilitados(false);
            
            System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("TEST 9: Message with spam (agents disabled)");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            boolean resultado9 = broker.publicar("canal_GENERAL", "SPAM spam SPAM urgent click here");
            System.out.println("Result: " + (resultado9 ? "ACCEPTED" : "REJECTED"));
            
            System.out.println("\nReactivating agents system ...");
            broker.setAgentesHabilitados(true);
            
        } catch (Exception e) {
            System.err.println("Error in test producer: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
