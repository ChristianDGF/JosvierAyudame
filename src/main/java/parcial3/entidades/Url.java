package parcial3.entidades;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.util.Date;

public class Url {
    @BsonId
    private ObjectId id;
    private String url;
    private String shortUrl; // Nombre corregido
    private Usuario usuario;
    private LocalDate fechaCreacion;

    // Constructor sin argumentos
    public Url() {

    }

    // Constructor con todos los campos
    public Url(ObjectId id, String url, String shortUrl, Usuario usuario) {
        this.id = id;
        this.url = url;
        this.shortUrl = shortUrl;
        this.usuario = usuario;
        this.fechaCreacion = LocalDate.now();
    }

    // Getters y Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDate getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDate fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

}