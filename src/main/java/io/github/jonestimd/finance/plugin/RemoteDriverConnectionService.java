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
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import javax.swing.JOptionPane;

import com.typesafe.config.Config;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.database.SuperUserDialog;

import static io.github.jonestimd.finance.plugin.DriverConfigurationService.Field.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;

public abstract class RemoteDriverConnectionService extends DriverConfigurationService {
    protected static final List<Field> FIELDS = unmodifiableList(asList(HOST, PORT, SCHEMA, USER, PASSWORD));

    protected RemoteDriverConnectionService(String name, String dialect, String driverClassName, String urlPrefix) {
        super(name, dialect, driverClassName, urlPrefix);
    }

    @Override
    public boolean isEnabled(Field field) {
        return FIELDS.contains(field);
    }

    @Override
    public boolean isRequired(Field field) {
        return FIELDS.contains(field);
    }

    @Override
    public Properties getHibernateProperties(Config config) {
        Properties properties = super.getHibernateProperties(config);
        properties.put("hibernate.dialect", dialect);
        properties.put("hibernate.connection.driver_class", driverClassName);
        properties.put("hibernate.connection.url", getJdbcUrl(config));
        properties.put("hibernate.connection.username", config.getString(USER.toString()));
        properties.put("hibernate.connection.password", config.getString(PASSWORD.toString()));
        return properties;
    }

    protected String getJdbcUrl(Config config) {
        return "jdbc:" + urlPrefix + config.getString(HOST.toString()) + ":" + config.getString(PORT.toString()) + "/" + config.getString(SCHEMA.toString());
    }

    protected abstract String getSetupJdbcUrl(String jdbcUrl);

    @Override
    protected boolean handleException(Config config, Consumer<String> updateProgress, SQLException ex) throws SQLException {
        Properties connectionProperties = getConnectionProperties(config);
        String message = ex.getMessage();
        String jdbcUrl = getJdbcUrl(config);
        if (needToCreateDatabase(message) && (! needSuperUser(message) || getSuperUser(message, jdbcUrl, connectionProperties))) {
            setupDatabase(config, jdbcUrl, connectionProperties, updateProgress);
            return true;
        }
        return ignoreError(message) || super.handleException(config, updateProgress, ex);
    }

    protected boolean ignoreError(String message) {
        return false;
    }

    protected abstract boolean needToCreateDatabase(String message);
    protected abstract boolean needSuperUser(String message);

    protected boolean getSuperUser(String message, String jdbcUrl, Properties connectionProperties) {
        SuperUserDialog dialog = new SuperUserDialog(JOptionPane.getRootFrame());
        do {
            dialog.setMessage(message);
            dialog.pack();
            dialog.setVisible(true);
            connectionProperties.setProperty(USER.toString(), dialog.getUser());
            connectionProperties.setProperty(PASSWORD.toString(), dialog.getPassword());
        } while (! dialog.isCancelled() && (message = testConnection(getSetupJdbcUrl(jdbcUrl), connectionProperties)) != null);
        return !dialog.isCancelled();
    }

    public String testConnection(Config config) {
        String message = testConnection(getJdbcUrl(config), getConnectionProperties(config));
        return message == null || ignoreError(message) || needToCreateDatabase(message)|| needSuperUser(message) ? null : message;
    }

    private String testConnection(String jdbcUrl, Properties connectionProperties) {
        try {
            DriverManager.getConnection(jdbcUrl, connectionProperties).close();
            return null;
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause != null && !(cause instanceof SQLException)) return cause.getMessage();
            return ex.getMessage();
        }
    }

    protected void setupDatabase(Config config, String jdbcUrl, Properties connectionProperties, Consumer<String> updateProgress) throws SQLException {
        updateProgress.accept(BundleType.LABELS.getString("database.status.creatingDatabase"));
        try (Connection connection = DriverManager.getConnection(jdbcUrl, connectionProperties)) {
            createUser(config, connection);
            createSchema(config, connection);
        }
    }

    protected abstract void createUser(Config config, Connection connection) throws SQLException;
    protected abstract void createSchema(Config config, Connection connection) throws SQLException;

    @Override
    protected Properties getConnectionProperties(Config config) {
        Properties properties = super.getConnectionProperties(config);
        copySetting(config, USER.toString(), properties);
        copySetting(config, PASSWORD.toString(), properties);
        return properties;
    }

    private void copySetting(Config config, String path, Properties properties) {
        if (config.hasPath(path)) properties.setProperty(path, config.getString(path));
    }

    @Override
    public Map<Field, String> getDefaultValues() {
        Map<Field, String> properties = new HashMap<>();
        properties.put(HOST, "localhost");
        properties.put(SCHEMA, "finances");
        properties.put(USER, "finances");
        return properties;
    }
}
