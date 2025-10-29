package com.ingsis.snippetManager;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SnippetServiceApplication {

    public static void main(String[] args) {
        // Cargar variables de .env 
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            
            // Establecer las variables como system properties
            if (dotenv != null) {
                dotenv.entries().forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value);
                    }
                });
            }
        } catch (Exception e) {
            // Si no hay .env, usar variables de entorno del sistema
        }
        
        SpringApplication.run(SnippetServiceApplication.class, args);
    }
}