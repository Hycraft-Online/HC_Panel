package com.hcpanel.database;

import com.hypixel.hytale.logger.HytaleLogger;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public class DatabaseManager {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("HC_Panel-DB");

    private final HikariDataSource dataSource;

    public DatabaseManager(String jdbcUrl, String username, String password, int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(1);
        config.setDriverClassName("org.postgresql.Driver");
        config.setConnectionTimeout(10000);
        config.setPoolName("HC_Panel-DB-Pool");

        this.dataSource = new HikariDataSource(config);
        LOGGER.at(Level.INFO).log("Database connection pool initialized");
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.at(Level.INFO).log("Database connection pool closed");
        }
    }
}
