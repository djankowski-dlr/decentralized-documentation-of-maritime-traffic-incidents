package de.dlr.ddomtia.configuration;

import com.mongodb.MongoClientURI;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class DatabaseConfig {
    private final String host;
    private final int port;
    private final String mongoDatabase;
    private final String username;
    private final String password;
    private final String collection;
    private final String authDB;

    public DatabaseConfig(@Value("${spring.data.mongodb.host}") String host,
                          @Value("${spring.data.mongodb.port}") int port,
                          @Value("${spring.data.mongodb.database}") String mongoDatabase,
                          @Value("${spring.data.mongodb.username}") String username,
                          @Value("${spring.data.mongodb.password}") String password,
                          @Value("${spring.data.mongodb.database.collection}") String collection,
                          @Value("${spring.data.mongodb.authentication-database}") String authDB) {
        this.host = host;
        this.port = port;
        this.mongoDatabase = mongoDatabase;
        this.username = username;
        this.password = password;
        this.collection = collection;
        this.authDB = authDB;
    }

    public MongoClientURI getURI() {
        return new MongoClientURI("mongodb://"+ this.username +":" + this.password + "@" + this.host + ":" + this.port + "/?authSource=" + this.authDB);
    }
}
