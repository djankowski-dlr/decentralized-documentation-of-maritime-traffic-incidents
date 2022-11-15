package de.dlr.ddomtia.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.mongodb.MongoClient;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.model.GridFSFile;

import de.dlr.ddomtia.configuration.DatabaseConfig;
import de.dlr.ddomtia.dto.TSADocument;
import de.dlr.ddomtia.util.Util;

import lombok.SneakyThrows;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

@Service
public final class MongoDBService {
    private final DatabaseConfig databaseConfig;
    private final MongoDatabase mongoDatabase;
    private final GridFsTemplate gridFsTemplate;
    private final GridFsOperations operations;

    @Autowired
    public MongoDBService(DatabaseConfig databaseConfig, GridFsTemplate gridFsTemplate, GridFsOperations operations) {
        this.databaseConfig = databaseConfig;
        final MongoClient mongoClient = new MongoClient(databaseConfig.getURI());
        this.mongoDatabase = mongoClient.getDatabase(databaseConfig.getMongoDatabase());
        this.gridFsTemplate = gridFsTemplate;
        this.operations = operations;
    }

    public void addTSADocument(TSADocument tsaDocument) {
        final String json = Util.gson.toJson(tsaDocument);
        this.mongoDatabase.getCollection(this.databaseConfig.getCollection()).insertOne(Document.parse(json));
    }

    @SneakyThrows
    public void addBytesToMongoDB(String fileName, byte[] inputBytes) {
        this.gridFsTemplate.store(new ByteArrayInputStream(inputBytes), fileName, "Bytes");
    }

    public byte[] getBytesFromMongoDB(String nameOfFile) throws IOException {
        final GridFSFile file = this.gridFsTemplate.findOne(new Query(Criteria.where(nameOfFile)));
        return this.operations.getResource(file).getInputStream().readAllBytes();
    }
}
