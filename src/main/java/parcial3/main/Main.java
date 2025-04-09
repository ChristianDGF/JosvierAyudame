package parcial3.main;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import parcial3.controladores.ApiAuthController;
import parcial3.controladores.ApiUrlController;
import parcial3.controladores.UrlController;
import parcial3.controladores.UserController;
import parcial3.servicios.UrlGrpcService;
import parcial3.entidades.Usuario;

public class Main {
    public static void main(String[] args) throws Exception {

        // Iniciar el servidor Javalin en el puerto 7000
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

        // Ruta raíz: redirige a las URLs si el usuario está logueado
        app.get("/", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (usuario == null) {
                ctx.redirect("/login");
                return;
            }
            ctx.redirect("/KonohaLinks/urls");
        });

        // Manejo de excepciones globales
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace(); // Imprime el error en consola
            ctx.status(500).result("Error: " + e.getMessage());
        });

        // Controladores existentes (HTML con Thymeleaf)
        new UserController().route(app);
        new UrlController().route(app);

        // Nuevos controladores REST con JWT
        new ApiAuthController().init(app);
        new ApiUrlController().init(app);

        // Levantar el servidor gRPC en el puerto 9090
        Server grpcServer = ServerBuilder.forPort(9090)
                .addService(new UrlGrpcService())
                .build();
        grpcServer.start();
        System.out.println("gRPC server started on port 9090");

        // Shutdown hook para cerrar el servidor gRPC al terminar la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gRPC server...");
            grpcServer.shutdown();
        }));

        // Bloquea el hilo principal para mantener vivo el servidor gRPC
        grpcServer.awaitTermination();
    }
}
