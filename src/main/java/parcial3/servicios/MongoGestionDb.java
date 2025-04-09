package parcial3.servicios;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.result.DeleteResult;
import static com.mongodb.client.model.Filters.eq;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.bson.codecs.configuration.CodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.*;
import org.bson.codecs.pojo.PojoCodecProvider;

public class MongoGestionDb<T> {

    // Shared client and database (like the static emf in your JPA class)
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    protected MongoCollection<T> collection;
    private Class<T> entityClass;

    /**
     * Constructor that initializes the MongoDB connection and selects the collection.
     * It reads the connection string and database name from environment variables if available.
     *
     * @param entityClass   the entity type class (POJO)
     * @param collectionName the MongoDB collection name where the entity is stored
     */
    public MongoGestionDb(Class<T> entityClass, String collectionName) {
        this.entityClass = entityClass;
        if (mongoClient == null) {
            // Read the connection string from the environment variable MONGODB_URL
            String connectionStringValue = System.getenv("MONGODB_URL");
            if (connectionStringValue == null || connectionStringValue.isEmpty()) {
                // Fallback to a default connection string with a default database name
                connectionStringValue = "mongodb+srv://christiandg1308:VOoGg1gGvu3KWwNm@cluster0.dobb5qz.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
            }
            ConnectionString connectionString = new ConnectionString(connectionStringValue);

            // Extract the database name from the connection string; fallback if necessary.
            String dbName = connectionString.getDatabase();
            if (dbName == null || dbName.isEmpty()) {
                dbName = "KonohaLinks";
            }

            // Setup CodecRegistry for POJOs
            CodecRegistry pojoCodecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .codecRegistry(pojoCodecRegistry)
                    .build();

            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(dbName);
        }
        // Get the collection for the entity type
        collection = database.getCollection(collectionName, entityClass);
    }

    /**
     * Inserts the given entity into the MongoDB collection.
     *
     * @param entidad the entity to insert
     * @return the inserted entity
     */
    public T crear(T entidad) {
        collection.insertOne(entidad);
        return entidad;
    }


    public T editar(T entidad) {
        Object id = getIdValue(entidad);
        if (id == null) {
            throw new IllegalArgumentException("El objeto entidad no tiene definido un id.");
        }
        String idFieldName = getIdFieldName();
        collection.replaceOne(eq(idFieldName, id), entidad);
        return entidad;
    }

    public T find(Object id) {
        String idFieldName = getIdFieldName();
        return collection.find(eq(idFieldName, id)).first();
    }

    public boolean eliminar(Object entidadId) {
        String idFieldName = getIdFieldName();
        DeleteResult result = collection.deleteOne(eq(idFieldName, entidadId));
        return result.getDeletedCount() > 0;
    }
    /**
     * Returns a list of all entities in the collection.
     *
     * @return list of all entities
     */
    public List<T> findAll() {
        List<T> list = new ArrayList<>();
        collection.find().into(list);
        return list;
    }

    /**
     * Helper method to get the value of the id field. It first attempts to retrieve a field named "id",
     * then "_id" if "id" is not found.
     *
     * @param entidad the entity from which to extract the id
     * @return the value of the id field, or null if not found
     */
    private Object getIdValue(T entidad) {
        if (entidad == null) {
            return null;
        }
        Class<?> clazz = entidad.getClass();
        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(org.bson.codecs.pojo.annotations.BsonId.class)) {
                    field.setAccessible(true);
                    try {
                        return field.get(entidad);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            }
            clazz = clazz.getSuperclass();  // Check superclasses as well
        }
        return null;
    }


    private String getIdFieldName() {
        if (entityClass == null) {
            return "_id"; // Fallback if entityClass is null
        }
        Field[] fields = entityClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(org.springframework.data.annotation.Id.class)) {
                return field.getName();
            }
        }
        return "_id"; // Fallback if no field annotated with @Id is found
    }

    /**
     * Closes the MongoDB client connection. Call this when your application is shutting down.
     */
    public static void cerrarConexion() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
        }
    }
}
