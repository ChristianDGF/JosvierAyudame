package parcial3.controladores;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import io.javalin.http.UnauthorizedResponse;
import org.bson.types.ObjectId;
import parcial3.entidades.Acceso;
import parcial3.entidades.Url;
import parcial3.entidades.Usuario;
import parcial3.servicios.MongoGestionDb;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class UrlController {

    private final MongoGestionDb<Url> urlDb;
    private final MongoGestionDb<Acceso> accesoDb;
    private final Random random = new Random();
    private static final String CARACTERES = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefhijklmnopqrstguvwxyz0123456789";
    private static final int LONGITUD_CODIGO = 7;

    public UrlController() {
        this.urlDb = new MongoGestionDb<>(Url.class, "urls");
        this.accesoDb = new MongoGestionDb<>(Acceso.class, "accesos");
    }

    public void route(Javalin app) {

        // Redirección y registro de acceso
        app.get("/{shortUrl}", ctx -> {
            String shortUrl = ctx.pathParam("shortUrl");

            // Buscar la URL original
            Url url = urlDb.findAll().stream()
                    .filter(u -> u.getShortUrl().equals(shortUrl))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundResponse("Enlace no encontrado"));

            // Crear registro de acceso
            Acceso acceso = new Acceso();
            acceso.setUrl(url);

            // Tomamos el User-Agent desde la cabecera
            String userAgent = ctx.header("User-Agent");
            if (userAgent == null) {
                userAgent = "";
            }
            acceso.setNavegador(obtenerNavegadorSimplificado(userAgent));
            acceso.setIp(ctx.ip());

            // Tomamos el Host desde la cabecera
            String hostHeader = ctx.header("Host");
            if (hostHeader == null || hostHeader.isEmpty()) {
                hostHeader = "localhost";
            }
            acceso.setDominio(obtenerDominio(hostHeader));
            acceso.setSistemaOperativo(obtenerSistemaOperativo(userAgent));
            acceso.setFecha(LocalDateTime.now());

            accesoDb.crear(acceso);

            // Redirigir a la URL original
            ctx.redirect(url.getUrl());
        });

        // Listar URLs (propias o todas si es admin)
        app.get("/KonohaLinks/urls/MisUrl", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (usuario == null) {
                ctx.redirect("/login");
                return;
            }

            List<Url> urls;
            if (usuario.isAdmin()) {
                urls = urlDb.findAll();
            } else {
                urls = urlDb.findAll().stream()
                        .filter(url -> url.getUsuario().getId().equals(usuario.getId()))
                        .toList();
            }

            Map<String, Object> model = new HashMap<>();
            model.put("urls", urls);

            ctx.render("/templates/listar-mis-urls.html", model);
        });

        // Listar URLs de la comunidad
        app.get("/KonohaLinks/urls", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");

            List<Url> urls = urlDb.findAll();

            Map<String, Object> model = new HashMap<>();
            model.put("urls", urls);
            model.put("usuario", usuario);

            if (usuario != null) {
                model.put("esAdmin", usuario.isAdmin());
            }

            ctx.render("/templates/listar-urls.html", model);
        });

        // Estadísticas de accesos (solo admin o dueño)
        app.get("/urls/{id}/accesos", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");

            ObjectId urlId = new ObjectId(ctx.pathParam("id"));
            Url url = urlDb.find(urlId);

            List<Acceso> accesos = accesoDb.findAll().stream()
                    .filter(a -> a.getUrl().getId().equals(urlId))
                    .toList();

            Map<String, Long> sistemasOperativos = accesos.stream()
                    .collect(Collectors.groupingBy(Acceso::getSistemaOperativo, Collectors.counting()));

            Map<String, Long> navegadores = accesos.stream()
                    .collect(Collectors.groupingBy(Acceso::getNavegador, Collectors.counting()));

            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> model = new HashMap<>();
            model.put("accesos", accesos);
            model.put("url", url);
            model.put("sistemasOperativosJson", mapper.writeValueAsString(sistemasOperativos));
            model.put("navegadoresJson", mapper.writeValueAsString(navegadores));

            // Renderiza la vista
            ctx.render("/templates/listar-accesos.html", model);
        });

        // Formulario para crear nueva URL
        app.get("/urls/crear", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (usuario == null) {
                ctx.redirect("/login");
                return;
            }
            ctx.render("/templates/crear-url.html");
        });

        // Procesar creación de URL
        app.post("/urls/crear", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (usuario == null) {
                throw new UnauthorizedResponse("Debe iniciar sesión");
            }

            String originalUrl = ctx.formParam("urlOriginal");
            String shortUrl = generarCodigoUnico();

            Url nuevaUrl = new Url();
            nuevaUrl.setUrl(originalUrl);
            nuevaUrl.setShortUrl(shortUrl);
            nuevaUrl.setUsuario(usuario);
            nuevaUrl.setFechaCreacion(LocalDate.now());

            urlDb.crear(nuevaUrl);
            ctx.redirect("/");
        });

        // Eliminar URL (solo administradores)
        app.post("/urls/eliminar/{id}", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (usuario == null) {
                throw new UnauthorizedResponse("Debe iniciar sesión");
            }
            // Restringir la eliminación de enlaces solo a admins
            if (!usuario.isAdmin()) {
                throw new UnauthorizedResponse("Solo administradores pueden eliminar enlaces");
            }

            ObjectId urlId = new ObjectId(ctx.pathParam("id"));
            urlDb.eliminar(urlId);

            ctx.redirect("/urls");
        });
    }

    /**
     * Genera un código corto único para la URL.
     */
    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = generarCodigoAleatorio();
        } while (urlExiste(codigo));
        return codigo;
    }

    private boolean urlExiste(String codigo) {
        return urlDb.findAll().stream()
                .anyMatch(url -> url.getShortUrl().equals(codigo));
    }

    private String generarCodigoAleatorio() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LONGITUD_CODIGO; i++) {
            int index = random.nextInt(CARACTERES.length());
            sb.append(CARACTERES.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Extrae el dominio desde la cabecera "Host".
     */
    private String obtenerDominio(String hostHeader) {
        // Si el Host inicia con "www.", lo removemos
        return hostHeader.startsWith("www.") ? hostHeader.substring(4) : hostHeader;
    }

    /**
     * Detecta el sistema operativo desde la cadena de User-Agent.
     */
    private String obtenerSistemaOperativo(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("mac")) return "MacOS";
        if (ua.contains("linux")) return "Linux";
        if (ua.contains("android")) return "Android";
        if (ua.contains("ios")) return "iOS";
        return "Desconocido";
    }

    /**
     * Retorna el navegador simplificado: Chrome, Firefox, Safari, etc.
     */
    private String obtenerNavegadorSimplificado(String userAgent) {
        String ua = userAgent.toLowerCase();
        if (ua.contains("chrome") && !ua.contains("edg")) {
            return "Chrome";
        } else if (ua.contains("firefox")) {
            return "Firefox";
        } else if (ua.contains("safari") && !ua.contains("chrome")) {
            return "Safari";
        } else if (ua.contains("edg")) {
            return "Edge";
        } else if (ua.contains("opera")) {
            return "Opera";
        } else if (ua.contains("crios")) { // Chrome en iOS
            return "Chrome Mobile";
        } else if (ua.contains("fxios")) { // Firefox en iOS
            return "Firefox Mobile";
        } else {
            return "Otro";
        }
    }
}
