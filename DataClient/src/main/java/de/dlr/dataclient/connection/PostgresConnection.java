package de.dlr.dataclient.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import de.dlr.dataclient.configuration.PostgresConfig;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public final class PostgresConnection {
    private final PostgresConfig postgresConfig;

    @Autowired
    public PostgresConnection(PostgresConfig postgresConfig) {
        this.postgresConfig = postgresConfig;
    }

    public Connection getConnection() throws SQLException {
        final Properties properties = new Properties();
        properties.setProperty("user", this.postgresConfig.getDatasourceUsername());
        properties.setProperty("password", this.postgresConfig.getDatasourcePassword());
        final String url = this.postgresConfig.getDatasourceUrl();
        return DriverManager.getConnection(url, properties);
    }
}
