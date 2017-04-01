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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.typesafe.config.Config;

import static io.github.jonestimd.finance.plugin.DriverConfigurationService.Field.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;

public abstract class RemoteDriverConnectionService implements DriverConfigurationService {
    protected static final List<Field> FIELDS = unmodifiableList(asList(HOST, PORT, SCHEMA, USER, PASSWORD));
    private final String name;
    private final String dialect;
    private final String driverClassName;
    private final String urlPrefix;

    protected RemoteDriverConnectionService(String name, String dialect, String driverClassName, String urlPrefix) {
        this.name = name;
        this.dialect = dialect;
        this.driverClassName = driverClassName;
        this.urlPrefix = urlPrefix;
    }

    @Override
    public String getName() {
        return name;
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
    public Properties getConnectionProperties(Config config) {
        Properties properties = new Properties();
        properties.put("hibernate.dialect", dialect);
        properties.put("hibernate.connection.driver_class", driverClassName);
        properties.put("hibernate.connection.url", getJdbcUrl(config));
        properties.put("hibernate.connection.username", config.getString("user"));
        properties.put("hibernate.connection.password", config.getString("password"));
        return properties;
    }

    protected String getJdbcUrl(Config config) {
        return "jdbc:" + urlPrefix + config.getString("host") + ":" + config.getString("port") + "/" + config.getString("schema");
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
