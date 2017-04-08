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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import com.typesafe.config.Config;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.database.SuperUserDialog;

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
    protected boolean handleException(Config config, Consumer<String> updateProgress, SQLException ex) throws SQLException {
        Properties connectionProperties = getConnectionProperties(config);
        if (needSuperUser(ex.getMessage().toLowerCase()) && getSuperUser(ex.getMessage(), connectionProperties)) {
            setupDatabase(config, connectionProperties, updateProgress, true);
            return true;
        }
        if (ex.getMessage().toLowerCase().startsWith("unknown database")) {
            setupDatabase(config, connectionProperties, updateProgress, false);
            return true;
        }
        return super.handleException(config, updateProgress, ex);
    }

    private boolean needSuperUser(String message) {
        return message.startsWith("access denied") || message.endsWith(".company' doesn't exist");
    }

    private boolean getSuperUser(String message, Properties connectionProperties) {
        SuperUserDialog dialog = new SuperUserDialog(JOptionPane.getRootFrame(), message);
        dialog.pack();
        dialog.setVisible(true);
        connectionProperties.setProperty(USER.toString(), dialog.getUser());
        connectionProperties.setProperty(PASSWORD.toString(), dialog.getPassword());
        return !dialog.isCancelled();
    }

    private void setupDatabase(Config config, Properties connectionProperties, Consumer<String> updateProgress, boolean createUser) throws SQLException {
        updateProgress.accept(BundleType.LABELS.getString("database.status.creatingDatabase"));
        String jdbcUrl = withoutSchema(getJdbcUrl(config));
        try (Connection connection = DriverManager.getConnection(jdbcUrl, connectionProperties)) {
            if (createUser) createUser(config, connection);
            createSchema(config, connection);
        }
    }

    private void createUser(Config config, Connection connection) throws SQLException {
        boolean withPassword = config.hasPath(PASSWORD.toString());
        StringBuilder sql = new StringBuilder("create user if not exists ?");
        if (withPassword) sql.append(" identified by ?");
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, config.getString(USER.toString()));
            if (withPassword) statement.setString(2, config.getString(PASSWORD.toString()));
            statement.execute();
        }
    }

    private void createSchema(Config config, Connection connection) throws SQLException {
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
