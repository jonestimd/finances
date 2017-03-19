// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
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
package io.github.jonestimd.finance.swing.database;

import java.io.File;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.jonestimd.util.Streams;

import static com.typesafe.config.ConfigUtil.*;

public class DatabaseConfig {
    public static final String TEMPLATE_CONFIG_PATH = "jdbc.options";
    public static final String TEMPLATE_NAME_PATH = "name";
    public static final String URL_ARGS_PATH = "url.args";
    public static final String URL_FORMAT_PATH = "url.format";
    public static final String PROPERTY_NAMES_PATH = "properties";
    public static final String DEFAULTS_PATH = "defaults";

    public static final String CONNECTION_URL_PATH = "url";
    public static final String CONNECTION_TYPE_PATH = "type";
    private Config template;
    private Map<String, Object> connectionConfig = new HashMap<>();

    public static List<DatabaseConfig> loadTemplates() {
        return Streams.map(ConfigFactory.load().getConfigList(TEMPLATE_CONFIG_PATH), DatabaseConfig::new);
    }

    public static Map<String, DatabaseConfig> load(String fileName) throws ParseException {
        Config connections = ConfigFactory.parseFile(new File(fileName));
        List<String> names = connections.entrySet().stream().map(Entry::getKey)
                .filter(key -> key.endsWith(".type"))
                .map(key -> key.replaceFirst("\\.type$", ""))
                .collect(Collectors.toList());
        Map<String, DatabaseConfig> configs = new TreeMap<>();
        for (String name : names) {
            configs.put(name, of(connections.getConfig(name)));
        }
        return configs;
    }

    public static DatabaseConfig of(Config connection) throws ParseException {
        String type = connection.getString(CONNECTION_TYPE_PATH);
        Optional<? extends Config> template = ConfigFactory.load().getConfigList(TEMPLATE_CONFIG_PATH).stream().filter(byName(type)).findFirst();
        if (template.isPresent()) {
            return new DatabaseConfig(template.get(), connection);
        }
        throw new RuntimeException("Invalid database type: " + type);
    }

    private static Predicate<Config> byName(String templateName) {
        return config -> config.getString(TEMPLATE_NAME_PATH).equals(templateName);
    }

    /**
     * Create a new configuration.
     * @param template the template on which the configuration is based
     */
    public DatabaseConfig(Config template) {
        this.template = template;
        for (String name : getParameterNames()) {
            connectionConfig.put(name, template.getValue(joinPath(DEFAULTS_PATH, name)).unwrapped());
        }
    }

    /**
     * Load an existing configuration.
     * @param template the template on which the configuration is based
     * @param connection the configuration to load
     * @throws ParseException if the driver URL is invalid
     */
    private DatabaseConfig(Config template, Config connection) throws ParseException {
        this.template = template;
        List<String> urlArgs = template.getStringList(URL_ARGS_PATH);
        Object[] args = getUrlFormat().parse(connection.getString(CONNECTION_URL_PATH));
        for (int i = 0; i < urlArgs.size(); i++) {
            put(urlArgs.get(i), args[i]);
        }
        for (String name : getPropertyNames()) {
            put(name, connection.getString(name));
        }
    }

    public Config getTemplate() {
        return template;
    }

    public boolean isEmbeddedDatabase() {
        return template.getString(TEMPLATE_NAME_PATH).equals("Derby");
    }

    private MessageFormat getUrlFormat() {
        return new MessageFormat(template.getString(URL_FORMAT_PATH));
    }

    public String getTemplateName() {
        return template.getString(TEMPLATE_NAME_PATH);
    }

    public boolean isSameName(DatabaseConfig other) {
        return getTemplateName().equals(other.getTemplateName());
    }

    public List<String> getParameterNames() {
        List<String> names = template.getStringList(URL_ARGS_PATH);
        names.addAll(getPropertyNames());
        return names;
    }

    private List<String> getPropertyNames() {
        return template.hasPath(PROPERTY_NAMES_PATH) ? template.getStringList(PROPERTY_NAMES_PATH) : Collections.emptyList();
    }

    public String get(String parameter) {
        return connectionConfig.get(parameter).toString();
    }

    public void put(String parameter, Object value) {
        connectionConfig.put(parameter, value);
    }

    /**
     * Convert the current configuration to a {@link Config}.
     * @param prefix the path prefix to use for the settings in the returned {@link Config}
     */
    public Config toConfig(String prefix) {
        Map<String, Object> configuration = new HashMap<>();
//        connectionConfig.forEach((key, value) -> configuration.put(joinPath(prefix, key), value));
        configuration.put(joinPath(prefix, CONNECTION_TYPE_PATH), getTemplateName());
        Object[] urlArgs = template.getStringList(URL_ARGS_PATH).stream().map(connectionConfig::get).toArray();
        configuration.put(joinPath(prefix, CONNECTION_URL_PATH), getUrlFormat().format(urlArgs));
        for (String name : getPropertyNames()) {
            configuration.put(joinPath(prefix, name), connectionConfig.get(name));
        }
        return ConfigFactory.parseMap(configuration);
    }
}
