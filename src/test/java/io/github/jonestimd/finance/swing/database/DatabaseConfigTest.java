package io.github.jonestimd.finance.swing.database;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class DatabaseConfigTest {
    @Test
    public void loadTemplates() throws Exception {
        List<? extends Config> configs = ConfigFactory.load().getConfigList("jdbc.options");
        List<DatabaseConfig> templates = DatabaseConfig.loadTemplates();

        assertThat(templates).hasSize(3);
        assertThat(templates.get(0).getTemplateName()).isEqualTo("Derby");
        assertThat(templates.get(0).getParameterNames()).containsExactly("directory");
        checkDefaults(configs.get(0), templates.get(0), "directory");
        assertThat(templates.get(1).getTemplateName()).isEqualTo("MySql");
        checkDefaults(configs.get(1), templates.get(1), "host", "port", "schema", "user", "password");
        assertThat(templates.get(2).getTemplateName()).isEqualTo("PostgreSQL");
        checkDefaults(configs.get(2), templates.get(2), "host", "port", "schema", "user", "password");
    }

    private void checkDefaults(Config config, DatabaseConfig template, String ...parameters) {
        assertThat(template.getParameterNames()).containsExactly(parameters);
        for (String parameter : parameters) {
            assertThat(template.get(parameter)).isEqualTo(config.getString("defaults." + parameter));
        }
    }

    @Test
    public void loadSavedConnections() throws Exception {
        String file = getClass().getResource("test-connections.conf").getFile();

        Map<String, DatabaseConfig> connections = DatabaseConfig.load(file);

        assertThat(connections).hasSize(3);
        assertThat(connections.get("prod").getTemplateName()).isEqualTo("MySql");
        assertThat(connections.get("prod").get("host")).isEqualTo("example.com");
        assertThat(connections.get("prod").get("port")).isEqualTo("3306");
        assertThat(connections.get("prod").get("schema")).isEqualTo("finances");
        assertThat(connections.get("prod").get("user")).isEqualTo("prod-finances");
        assertThat(connections.get("prod").get("password")).isEqualTo("prod-password");
        assertThat(connections.get("test.mysql").getTemplateName()).isEqualTo("MySql");
        assertThat(connections.get("test.mysql").get("host")).isEqualTo("localhost");
        assertThat(connections.get("test.mysql").get("port")).isEqualTo("3308");
        assertThat(connections.get("test.mysql").get("user")).isEqualTo("test-finances");
        assertThat(connections.get("test.mysql").get("password")).isEqualTo("test-password");
        assertThat(connections.get("test.derby").getTemplateName()).isEqualTo("Derby");
        assertThat(connections.get("test.derby").get("directory")).isEqualTo("/home/user/finances");
    }

    @Test(expected = RuntimeException.class)
    public void exceptionForInvalidType() throws Exception {
        Config config = ConfigFactory.parseMap(ImmutableMap.of("type", "Hsql"));

        DatabaseConfig.of(config);
    }

    @Test
    public void toConfig() throws Exception {
        DatabaseConfig databaseConfig = DatabaseConfig.loadTemplates().get(1);
        databaseConfig.put("host", "example.com");
        databaseConfig.put("port", 3306);
        databaseConfig.put("schema", "finances");
        databaseConfig.put("user", "user ID");
        databaseConfig.put("password", "PassWord");

        Config config = databaseConfig.toConfig("name");

        assertThat(config.getString("name.type")).isEqualTo("MySql");
        assertThat(config.getString("name.url")).isEqualTo("jdbc:mysql://example.com:3306/finances");
        assertThat(config.getString("name.user")).isEqualTo("user ID");
        assertThat(config.getString("name.password")).isEqualTo("PassWord");
    }
}