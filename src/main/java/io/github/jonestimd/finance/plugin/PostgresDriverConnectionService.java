// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import com.typesafe.config.Config;

import static io.github.jonestimd.finance.plugin.DriverConfigurationService.Field.*;

public class PostgresDriverConnectionService extends RemoteDriverConnectionService {
    public PostgresDriverConnectionService() {
        super("PostgreSQL", "org.hibernate.dialect.PostgreSQL9Dialect", "org.postgresql.Driver", "postgresql://");
    }

    @Override
    public Map<Field, String> getDefaultValues() {
        Map<Field, String> properties = super.getDefaultValues();
        properties.put(Field.PORT, "5432");
        return properties;
    }

    @Override
    protected boolean ignoreError(String message) {
        return message.startsWith("error: relation \"company\" does not exist") || super.ignoreError(message);
    }

    @Override
    protected boolean needToCreateDatabase(String message) {
        return needSuperUser(message);
    }

    @Override
    protected boolean needSuperUser(String message) {
        return message.startsWith("fatal: password authentication failed for user") || message.matches("^fatal: database (.*) does not exist$");
    }

    @Override
    protected void setupDatabase(Config config, String jdbcUrl, Properties connectionProperties, Consumer<String> updateProgress) throws SQLException {
        super.setupDatabase(config, withoutSchema(jdbcUrl), connectionProperties, updateProgress);
    }

    @Override
    protected void createUser(Config config, Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select usename from pg_catalog.pg_user where usename = ?")) {
            statement.setString(1, config.getString(USER.toString()));
            if (statement.executeQuery().next()) return;
        }
        try (Statement statement = connection.createStatement()) {
            StringBuilder sql = new StringBuilder("create user ").append(config.getString(USER.toString()));
            if (config.hasPath(PASSWORD.toString())) {
                sql.append(" with password '").append(config.getString(PASSWORD.toString())).append("'");
            }
            statement.execute(sql.toString());
        }
    }

    @Override
    protected void createSchema(Config config, Connection connection) throws SQLException {
        String schema = config.getString(SCHEMA.toString());
        try (PreparedStatement statement = connection.prepareStatement("select * from pg_catalog.pg_database where datname = ?")) {
            statement.setString(1, schema);
            if (statement.executeQuery().next()) return;
        }
        String user = config.getString(USER.toString());
        try (Statement statement = connection.createStatement()) {
            statement.execute("create database " + schema);
            statement.execute("grant all on database " + schema + " to " + user);
        }
    }

    private String withoutSchema(String jdbcUrl) {
        return jdbcUrl.substring(0, jdbcUrl.lastIndexOf('/') + 1);
    }
}
