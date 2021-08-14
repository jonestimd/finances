// The MIT License (MIT)
//
// Copyright (c) 2021 Tim Jones
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.typesafe.config.Config;
import io.github.jonestimd.finance.config.ConnectionConfig;
import io.github.jonestimd.util.Streams;

/**
 * The interface for mapping configuration values to Hibernate connection properties.  This interface is used for the
 * connection settings UI and for parsing connection settings.
 */
public abstract class DriverConfigurationService {
    private static final String HIBERNATE_PATH = "hibernate";

    /** The available configuration settings for a driver. */
    public enum Field {
        DRIVER, DIRECTORY, HOST, PORT, SCHEMA, USER, PASSWORD;

        public String toString() {
            return name().toLowerCase();
        }
    }
    protected final String name;
    protected final String dialect;
    protected final String driverClassName;
    protected final String urlPrefix;

    protected DriverConfigurationService(String name, String dialect, String driverClassName, String urlPrefix) {
        this.name = name;
        this.dialect = dialect;
        this.driverClassName = driverClassName;
        this.urlPrefix = urlPrefix;
    }

    /**
     * @return a unique name for the database driver.
     */
    public String getName() {
        return name;
    }

    public boolean isAvailable() {
        try {
            Class.forName(driverClassName);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * @param field a configuration setting
     * @return true if the setting is used by this database driver
     */
    public abstract boolean isEnabled(Field field);

    /**
     * @param field a configuration setting
     * @return true if the setting is required by this database driver
     */
    public abstract boolean isRequired(Field field);

    /**
     * Get the Hibernate connection properties for this driver.
     * @param config the driver configuration settings
     * @return Hibernate connection properties
     */
    public Properties getHibernateProperties(Config config) {
        return ConnectionConfig.asProperties(config, HIBERNATE_PATH);
    }

    public List<String> getHibernateResources() {
        return Collections.emptyList();
    }

    /**
     * @return default values for the driver settings
     */
    public abstract Map<Field, String> getDefaultValues();

    /**
     * Test the database connection properties.  Should ignore non-connection errors (e.g. invalid user or schema).
     * @param config configuration containing the connection parameters
     * @return the error message for a connection failure or null for success
     */
    public abstract String testConnection(Config config);

    /**
     * Ensure the database is ready to be used.  For example, create it if it does not exist.
     * @param config the connection configuration
     * @param updateProgress used to provide progress feedback on the UI
     * @return true if the tables need to be created and initialized
     */
    public boolean prepareDatabase(Config config, Consumer<String> updateProgress) throws Exception {
        Class.forName(driverClassName);
        try (Connection connection = DriverManager.getConnection(getJdbcUrl(config), getConnectionProperties(config))) {
            connection.prepareStatement("select * from company").getMetaData();
        } catch (SQLException ex) {
            return handleException(config, updateProgress, ex);
        }
        return false;
    }

    protected abstract String getJdbcUrl(Config config);

    protected Properties getConnectionProperties(Config config) {
        return new Properties();
    }

    public List<String> getPostCreateSchemaScript() {
        return Collections.emptyList();
    }

    /**
     * Perform database setup based on the exception.
     * @param config the connection configuration
     * @param updateProgress callback to provide user feedback
     * @param ex the exception from the query to test for the database existance
     * @return true if the database is ready for tables to be created
     */
    protected boolean handleException(Config config, Consumer<String> updateProgress, SQLException ex) throws Exception {
        throw ex;
    }

    /**
     * Get the list of available drivers.
     */
    public static List<DriverConfigurationService> getServices() {
        return Streams.of(ServiceLoader.load(DriverConfigurationService.class))
                .filter(DriverConfigurationService::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * Get an instance of {@link DriverService} based on a driver configuration.  The {@code config}
     * must include a {@code type} setting that matches the value returned by {@link #getName()} for the driver.
     * @param config the driver configuration, including the name of the driver service
     * @return a {@link DriverService} or null if none match the {@code type} in {@code config}
     */
    public static DriverService forConfig(Config config) {
        String driver = config.getString(Field.DRIVER.toString());
        return getServices().stream().filter(service -> driver.equals(service.getName()))
                .findFirst()
                .map(service -> new DriverService(service, config))
                .orElse(null);
    }

    public static class DriverService {
        private final DriverConfigurationService service;
        private final Config config;

        public DriverService(DriverConfigurationService service, Config config) {
            this.service = service;
            this.config = config;
        }

        public DriverConfigurationService getDriverService() {
            return service;
        }

        public Properties getHibernateProperties() {
            return service.getHibernateProperties(config);
        }

        public List<String> getHibernateResources() {
            return service.getHibernateResources();
        }

        public boolean prepareDatabase(Consumer<String> updateProgress) throws Exception {
            return service.prepareDatabase(config, updateProgress);
        }

        public List<String> getPostCreateSchemaScript() {
            return service.getPostCreateSchemaScript();
        }
    }
}
