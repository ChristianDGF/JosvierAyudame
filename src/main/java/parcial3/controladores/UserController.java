package parcial3.controladores;

import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.bson.types.ObjectId;
import parcial3.entidades.Usuario;
import parcial3.servicios.MongoGestionDb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserController {

    private final MongoGestionDb<Usuario> usuarioDb;

    public UserController() {
        this.usuarioDb = new MongoGestionDb<>(Usuario.class, "usuarios");
        // Automatizar la creación del usuario administrador
        if (usuarioDb.findAll().stream().noneMatch(Usuario::isAdmin)) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setUsername("admin");
            admin.setPassword("admin"); // Define una contraseña segura en producción
            admin.setAdmin(true);
            usuarioDb.crear(admin);
        }
    }

    public void route(Javalin app) {

        app.post("/clear-login-error", ctx -> {
            ctx.sessionAttribute("loginError", null);
            ctx.status(200);
        });

        app.get("/login", ctx -> {
            ctx.render("/templates/login.html");
        });

        app.get("/registro", ctx -> {
            ctx.render("/templates/registro.html");
        });

        // Listar todos los usuarios (solo admin)
        app.get("/usuarios", ctx -> {
            Usuario usuarioSession = ctx.sessionAttribute("usuario");
            if (usuarioSession == null || !usuarioSession.isAdmin()) {
                throw new UnauthorizedResponse("Acceso denegado");
            }
            List<Usuario> usuarios = usuarioDb.findAll();
            Map<String, Object> model = new HashMap<>();
            model.put("usuarios", usuarios);
            ctx.render("/templates/listar-usuarios.html", model);
        });

        // Formulario de edición de usuario
        app.get("/usuarios/editar/{id}", ctx -> {
            Usuario usuarioSession = ctx.sessionAttribute("usuario");
            if (usuarioSession == null || !usuarioSession.isAdmin()) {
                throw new UnauthorizedResponse("Acceso denegado");
            }
            String id = ctx.pathParam("id");
            Usuario usuario = usuarioDb.find(new ObjectId(id));
            Map<String, Object> model = new HashMap<>();
            model.put("usuario", usuario);
            ctx.render("/templates/editar-usuario.html", model);
        });

        // Procesar edición de usuario
        app.post("/usuarios/editar/{id}", ctx -> {
            Usuario usuarioSession = ctx.sessionAttribute("usuario");
            if (usuarioSession == null || !usuarioSession.isAdmin()) {
                throw new UnauthorizedResponse("Acceso denegado");
            }
            String id = ctx.pathParam("id");
            Usuario usuario = usuarioDb.find(new ObjectId(id));
            usuario.setNombre(ctx.formParam("nombre"));
            usuario.setUsername(ctx.formParam("username"));
            usuario.setPassword(ctx.formParam("password"));
            usuario.setAdmin(Boolean.parseBoolean(ctx.formParam("admin")));
            usuarioDb.editar(usuario);
            ctx.redirect("/usuarios");
        });

        // Ruta para promover a un usuario a administrador (solo admin)
        app.post("/usuarios/promover/{id}", ctx -> {
            Usuario usuarioSession = ctx.sessionAttribute("usuario");
            if (usuarioSession == null || !usuarioSession.isAdmin()) {
                throw new UnauthorizedResponse("Acceso denegado");
            }
            String id = ctx.pathParam("id");
            Usuario usuario = usuarioDb.find(new ObjectId(id));
            if (usuario == null) {
                throw new NotFoundResponse("Usuario no encontrado");
            }
            // Convertir el usuario a admin
            usuario.setAdmin(true);
            usuarioDb.editar(usuario);
            ctx.redirect("/usuarios");
        });

        // Eliminar usuario (solo admin pueden eliminar, y ningún administrador se puede eliminar)
        app.post("/usuarios/eliminar/{id}", ctx -> {
            Usuario usuarioSession = ctx.sessionAttribute("usuario");
            if (usuarioSession == null || !usuarioSession.isAdmin()) {
                throw new UnauthorizedResponse("Acceso denegado");
            }
            String id = ctx.pathParam("id");
            Usuario usuario = usuarioDb.find(new ObjectId(id));
            if (usuario == null) {
                throw new NotFoundResponse("Usuario no encontrado");
            }
            if (usuario.isAdmin()) {
                // Impedir eliminar administradores
                throw new UnauthorizedResponse("No se puede eliminar un usuario administrador");
            }
            usuarioDb.eliminar(new ObjectId(id));
            ctx.redirect("/usuarios");
        });

        // Login
        app.post("/login", ctx -> {
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");
            Usuario usuario = usuarioDb.findAll().stream()
                    .filter(u -> u.getUsername().equals(username) && u.getPassword().equals(password))
                    .findFirst()
                    .orElse(null);
            if (usuario != null) {
                ctx.sessionAttribute("usuario", usuario);
                ctx.redirect("/");
            } else {
                ctx.sessionAttribute("loginError", "true");
                ctx.redirect("/login");
            }
        });

        // Registro de nuevos usuarios (se crean como no admin)
        app.post("/registro", ctx -> {
            String nombre = ctx.formParam("nombre");
            String username = ctx.formParam("username");
            String password = ctx.formParam("password");
            boolean usuarioExiste = usuarioDb.findAll().stream()
                    .anyMatch(u -> u.getUsername().equals(username));
            if (!usuarioExiste) {
                Usuario nuevoUsuario = new Usuario();
                nuevoUsuario.setNombre(nombre);
                nuevoUsuario.setUsername(username);
                nuevoUsuario.setPassword(password);
                nuevoUsuario.setAdmin(false);
                usuarioDb.crear(nuevoUsuario);
                ctx.sessionAttribute("usuario", nuevoUsuario);
                ctx.redirect("/");
            } else {
                ctx.status(409).result("El usuario ya existe");
            }
        });

        // Logout
        app.get("/logout", ctx -> {
            ctx.req().getSession().invalidate();
            ctx.redirect("/login");
        });
    }
}
