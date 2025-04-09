package parcial3.entidades;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

public class Usuario {
    @BsonId
    private ObjectId id;
    private String codigo;
    private String username;
    private String password;
    private String nombre;
    private boolean admin;

    // Constructor sin argumentos (requerido por MongoDB)
    public Usuario() {
    }

    // Constructor con todos los campos
    public Usuario(ObjectId id, String username, String password, String nombre, boolean admin) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.nombre = nombre;
        this.admin = admin;
    }

    // Getters y Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}