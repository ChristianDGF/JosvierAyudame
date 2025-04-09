package parcial3.controladores;

import io.javalin.Javalin;
import io.javalin.http.Context;
import parcial3.entidades.Usuario;
import parcial3.servicios.JWTService;
import parcial3.servicios.MongoGestionDb;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para autenticación (JWT).
 */
public class ApiAuthController {

    private final MongoGestionDb<Usuario> usuarioDb;

    public ApiAuthController() {
        this.usuarioDb = new MongoGestionDb<>(Usuario.class, "usuarios");
    }

    public void init(Javalin app) {

        // Login via JSON: { "username": "...", "password": "..." }
        app.post("/api/v1/login", ctx -> {
            Map<String, String> json = ctx.bodyAsClass(Map.class);
            String username = json.get("username");
            String password = json.get("password");

            Usuario usuario = usuarioDb.findAll().stream()
                    .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                    .findFirst()
                    .orElse(null);

            if (usuario == null) {
                ctx.status(401).json(Map.of("error", "Credenciales inválidas."));
                return;
            }

            // Generar un JWT
            String token = JWTService.generateToken(usuario.getUsername());

            // Retornar el token en JSON
            ctx.json(Map.of("token", token));
        });

        // (Opcional) Crear usuario via REST:
        app.post("/api/v1/register", ctx -> {
            Map<String, String> json = ctx.bodyAsClass(Map.class);
            String nombre = json.get("nombre");
            String username = json.get("username");
            String password = json.get("password");

            // Verificar si existe
            boolean existe = usuarioDb.findAll().stream()
                    .anyMatch(u -> u.getUsername().equals(username));
            if (existe) {
                ctx.status(409).json(Map.of("error", "Usuario ya existe"));
                return;
            }

            Usuario nuevo = new Usuario();
            nuevo.setNombre(nombre);
            nuevo.setUsername(username);
            nuevo.setPassword(password);
            nuevo.setAdmin(false); // o lo que desees

            usuarioDb.crear(nuevo);
            ctx.json(Map.of("status", "ok", "message", "Usuario creado"));
        });
    }
}
