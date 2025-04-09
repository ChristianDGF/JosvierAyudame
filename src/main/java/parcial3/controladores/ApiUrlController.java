package parcial3.controladores;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import org.bson.types.ObjectId;
import parcial3.entidades.Url;
import parcial3.entidades.Usuario;
import parcial3.servicios.JWTService;
import parcial3.servicios.MongoGestionDb;

import java.time.LocalDate;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class ApiUrlController {

    private final MongoGestionDb<Url> urlDb;
    private final MongoGestionDb<Usuario> usuarioDb;

    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefhijklmnopqrstguvwxyz0123456789";
    private static final int LONGITUD_CODIGO = 7;
    private final Random random = new Random();

    public ApiUrlController() {
        urlDb = new MongoGestionDb<>(Url.class, "urls");
        usuarioDb = new MongoGestionDb<>(Usuario.class, "usuarios");
    }

    public void init(Javalin app) {
        // Middleware para verificar JWT en rutas /api/v1/urls
        app.before("/api/v1/urls/*", ctx -> {
            // Excepto si es un OPTIONS (CORS) ...
            if (ctx.method() == HandlerType.OPTIONS) {
                return;
            }
            verificarToken(ctx);
        });

        // Listar URLs del usuario logueado
        app.get("/api/v1/urls", ctx -> {
            // Se asume que el token ya se validó en before()
            String username = ctx.attribute("username");
            // Buscar el usuario en BD
            Usuario usuario = usuarioDb.findAll().stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new UnauthorizedResponse("Usuario no encontrado."));

            // Si es admin, retorna todas, si no, solo las suyas
            if (usuario.isAdmin()) {
                ctx.json(urlDb.findAll());
            } else {
                ctx.json(urlDb.findAll().stream()
                        .filter(u -> u.getUsuario().getId().equals(usuario.getId()))
                        .collect(Collectors.toList()));
            }
        });

        // Crear nueva URL acortada
        app.post("/api/v1/urls", ctx -> {
            String username = ctx.attribute("username");
            Usuario usuario = usuarioDb.findAll().stream()
                    .filter(u -> u.getUsername().equals(username))
                    .findFirst()
                    .orElseThrow(() -> new UnauthorizedResponse("Usuario no encontrado."));

            Map<String, String> body = ctx.bodyAsClass(Map.class);
            String urlOriginal = body.get("urlOriginal");

            String shortUrl = generarCodigoUnico();

            Url nuevaUrl = new Url();
            nuevaUrl.setUrl(urlOriginal);
            nuevaUrl.setShortUrl(shortUrl);
            nuevaUrl.setUsuario(usuario);
            nuevaUrl.setFechaCreacion(LocalDate.now());

            urlDb.crear(nuevaUrl);

            // Retornamos la URL recién creada
            ctx.json(nuevaUrl);
        });
    }

    private void verificarToken(Context ctx) {
        String authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Token no provisto.");
        }

        String token = authHeader.substring(7); // quitar "Bearer "
        String username;
        try {
            username = JWTService.validateToken(token);
        } catch (Exception e) {
            throw new UnauthorizedResponse("Token inválido o expirado.");
        }

        // Guardamos el "username" en el contexto para usarlo en las rutas
        ctx.attribute("username", username);
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = generarCodigoAleatorio();
        } while (urlExiste(codigo));
        return codigo;
    }

    private boolean urlExiste(String codigo) {
        return urlDb.findAll().stream().anyMatch(u -> u.getShortUrl().equals(codigo));
    }

    private String generarCodigoAleatorio() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LONGITUD_CODIGO; i++) {
            int index = random.nextInt(CARACTERES.length());
            sb.append(CARACTERES.charAt(index));
        }
        return sb.toString();
    }
}
