package parcial3.servicios;

import io.grpc.stub.StreamObserver;
import org.bson.types.ObjectId;
import parcial3.entidades.Url;
import parcial3.entidades.Usuario;
import parcial3.grpc.CreateUrlRequest;
import parcial3.grpc.CreateUrlResponse;
import parcial3.grpc.ListUrlsRequest;
import parcial3.grpc.ListUrlsResponse;
import parcial3.grpc.UrlData;
import parcial3.grpc.UrlServiceGrpc;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio gRPC definido en url.proto
 */
public class UrlGrpcService extends UrlServiceGrpc.UrlServiceImplBase {

    private final MongoGestionDb<Url> urlDb;
    private final MongoGestionDb<Usuario> usuarioDb;

    public UrlGrpcService() {
        this.urlDb = new MongoGestionDb<>(Url.class, "urls");
        this.usuarioDb = new MongoGestionDb<>(Usuario.class, "usuarios");
    }

    @Override
    public void listUrls(
            ListUrlsRequest request,
            StreamObserver<ListUrlsResponse> responseObserver
    ) {
        String userId = request.getUserId();

        // Filtrar las URLs
        List<Url> allUrls = urlDb.findAll();
        List<Url> filtered;
        if (userId == null || userId.isEmpty()) {
            filtered = allUrls;
        } else {
            filtered = allUrls.stream()
                    .filter(u -> u.getUsuario().getId().toHexString().equals(userId))
                    .collect(Collectors.toList());
        }

        // Convertimos cada Url a UrlData
        List<UrlData> dataList = filtered.stream()
                .map(this::toUrlData)
                .collect(Collectors.toList());

        // Construimos la respuesta
        ListUrlsResponse response = ListUrlsResponse.newBuilder()
                .addAllUrls(dataList)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void createUrl(
            CreateUrlRequest request,
            StreamObserver<CreateUrlResponse> responseObserver
    ) {
        String userId = request.getUserId();
        String urlOriginal = request.getUrlOriginal();

        // Buscar el usuario
        Usuario usuario = usuarioDb.findAll().stream()
                .filter(u -> u.getId().toHexString().equals(userId))
                .findFirst()
                .orElse(null);

        if (usuario == null) {
            responseObserver.onError(new RuntimeException("Usuario no encontrado"));
            return;
        }

        // Crear la nueva URL
        Url nueva = new Url();
        nueva.setUrl(urlOriginal);
        nueva.setShortUrl("grpc-" + System.currentTimeMillis());
        nueva.setUsuario(usuario);
        nueva.setFechaCreacion(java.time.LocalDate.now());
        urlDb.crear(nueva);

        UrlData data = toUrlData(nueva);

        CreateUrlResponse response = CreateUrlResponse.newBuilder()
                .setUrl(data)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private UrlData toUrlData(Url url) {
        return UrlData.newBuilder()
                .setId(url.getId().toHexString())
                .setUrl(url.getUrl() != null ? url.getUrl() : "")
                .setShortUrl(url.getShortUrl() != null ? url.getShortUrl() : "")
                .setUserId(url.getUsuario().getId().toHexString())
                .setFechaCreacion(
                        url.getFechaCreacion() != null
                                ? url.getFechaCreacion().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                : ""
                )
                .build();
    }
}
