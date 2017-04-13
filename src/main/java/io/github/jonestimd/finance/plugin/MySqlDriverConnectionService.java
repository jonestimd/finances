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

public class MySqlDriverConnectionService extends RemoteDriverConnectionService {
    public MySqlDriverConnectionService() {
        super("MySql", "org.hibernate.dialect.MySQL5InnoDBDialect", "com.mysql.jdbc.Driver", "mysql://");
    }

    @Override
    public Map<Field, String> getDefaultValues() {
        Map<Field, String> properties = super.getDefaultValues();
        properties.put(Field.PORT, "3306");
        return properties;
    }

    @Override
    protected boolean needToCreateDatabase(String message) {
        return message.startsWith("unknown database") || needSuperUser(message);
    }

    @Override
    protected boolean needSuperUser(String message) {
        return message.startsWith("access denied") || message.endsWith(".company' doesn't exist");
    }

    @Override
    protected void setupDatabase(Config config, String jdbcUrl, Properties connectionProperties, Consumer<String> updateProgress) throws SQLException {
        super.setupDatabase(config, withoutSchema(jdbcUrl), connectionProperties, updateProgress);
    }

    @Override
    protected void createUser(Config config, Connection connection) throws SQLException {
        boolean withPassword = config.hasPath(PASSWORD.toString());
        StringBuilder sql = new StringBuilder("create user if not exists ?");
        if (withPassword) sql.append(" identified by ?");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, config.getString(USER.toString()));
            if (withPassword) statement.setString(2, config.getString(PASSWORD.toString()));
            statement.execute();
        }
    }

    @Override
    protected void createSchema(Config config, Connection connection) throws SQLException {
        String schema = config.getString(SCHEMA.toString());
        String user = config.getString(USER.toString());
        try (Statement statement = connection.createStatement()) {
            statement.execute("create database if not exists " + schema);
            statement.execute("grant all on " + schema + ".* to " + user);
        }
    }

    private String withoutSchema(String jdbcUrl) {
        return jdbcUrl.substring(0, jdbcUrl.lastIndexOf('/'));
    }
}
