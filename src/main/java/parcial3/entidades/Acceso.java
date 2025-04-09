package parcial3.entidades;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

public class Acceso {
    @BsonId
    private ObjectId id;
    private Url url;
    private String navegador;
    private String ip;
    private String dominio;
    private String sistemaOperativo;
    private LocalDateTime fecha;

    // Constructor sin argumentos
    public Acceso() {
    }

    // Constructor con todos los campos
    public Acceso(ObjectId id, Url url, String navegador, String ip, String dominio, String sistemaOperativo) {
        this.id = id;
        this.url = url;
        this.navegador = navegador;
        this.ip = ip;
        this.dominio = dominio;
        this.sistemaOperativo = sistemaOperativo;
        this.fecha = LocalDateTime.now();
    }

    // Getters y Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Url getUrl() {
        return url;
    }

    public void setUrl(Url url) {
        this.url = url;
    }

    public String getNavegador() {
        return navegador;
    }

    public void setNavegador(String navegador) {
        this.navegador = navegador;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDominio() {
        return dominio;
    }

    public void setDominio(String dominio) {
        this.dominio = dominio;
    }

    public String getSistemaOperativo() {
        return sistemaOperativo;
    }

    public void setSistemaOperativo(String sistemaOperativo) {
        this.sistemaOperativo = sistemaOperativo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}