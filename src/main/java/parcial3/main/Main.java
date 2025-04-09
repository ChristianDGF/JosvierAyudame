package parcial3.main;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import parcial3.controladores.UrlController;
import parcial3.controladores.UserController;
import parcial3.entidades.Usuario;
import parcial3.entidades.Url;
import parcial3.entidades.Acceso;
import parcial3.servicios.MongoGestionDb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {


            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/";
                staticFileConfig.directory = "/public";
                staticFileConfig.location = Location.CLASSPATH;
                staticFileConfig.precompress = false;
                staticFileConfig.aliasCheck = null;
            });


            config.fileRenderer(new JavalinThymeleaf());

        }).start(7000);

        app.get("/", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");

            if (usuario == null) {
                ctx.redirect("/login");
                return;
            }

            ctx.redirect("/KonohaLinks/urls");

        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace(); // Ver el error en consola
            ctx.status(500).result("Error: " + e.getMessage());
        });

        new UserController().route(app);
        new UrlController().route(app);


    }
}