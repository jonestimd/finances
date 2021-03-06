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
package io.github.jonestimd.finance.config;

import java.io.File;
import java.util.Properties;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.github.jonestimd.finance.plugin.DerbyDriverConnectionService;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.DriverService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ConnectionConfigTest {
    private String userHome;

    @Before
    public void saveUserHome() {
        this.userHome = System.getProperty("user.home");
        ConfigFactory.invalidateCaches();
    }

    @After
    public void resetProperties() {
        System.clearProperty(ConnectionConfig.CONNECTION_FILE_PROPERTY);
        System.setProperty("user.home", userHome);
    }

    @Test
    public void loadsFileSpecifiedByConfigProperty() throws Exception {
        String path = getClass().getResource("connection.conf").getPath();
        System.setProperty(ConnectionConfig.CONNECTION_FILE_PROPERTY, path);

        ConnectionConfig manager = new ConnectionConfig();

        assertThat(manager.get(ConnectionConfig.CONNECTION_PATH).get().getString("type")).isEqualTo("Specific");
    }

    @Test
    public void loadDefaultFileIfPropertyNotSet() throws Exception {
        String path = getClass().getResource("connection.conf").getPath();
        System.setProperty("user.home", new File(path).getParent());

        ConnectionConfig manager = new ConnectionConfig();

        assertThat(manager.get(ConnectionConfig.CONNECTION_PATH).get().getString("type")).isEqualTo("Default");
    }

    @Test
    public void returnsEmptyConfigIfFileNotFound() throws Exception {
        String path = getClass().getResource("/log4j.test.properties").getPath();
        System.setProperty("user.home", new File(path).getParent());

        ConnectionConfig manager = new ConnectionConfig();

        assertThat(manager.root().entrySet()).isEmpty();
    }

    @Test
    public void getReturnsEmptyOptionForMissingPath() throws Exception {
        String path = getClass().getResource("test.conf").getPath();
        ConnectionConfig manager = new ConnectionConfig("x", path);

        assertThat(manager.get("unknown").isPresent()).isFalse();
    }

    @Test
    public void getReturnsOptionForPath() throws Exception {
        String path = getClass().getResource("test.conf").getPath();
        ConnectionConfig manager = new ConnectionConfig("x", path);

        assertThat(manager.get("connection").isPresent()).isTrue();
    }

    @Test
    public void addPathAddsToConfiguration() throws Exception {
        String path = getClass().getResource("test.conf").getPath();
        ConnectionConfig manager = new ConnectionConfig("x", path);
        assertThat(manager.get("unknown").isPresent()).isFalse();

        manager.addPath("unknown", ConfigFactory.empty().withValue("setting", ConfigValueFactory.fromAnyRef("value")));

        assertThat(manager.get("unknown").isPresent()).isTrue();
        assertThat(manager.get("unknown").get().getString("setting")).isEqualTo("value");
    }

    @Test
    public void loadDriverUsesConfigurationFile() throws Exception {
        String path = getClass().getResource("driver.conf").getPath();
        ConnectionConfig config = new ConnectionConfig("config", path);

        DriverService service = config.loadDriver();

        assertThat(service.getDriverService()).isInstanceOf(DerbyDriverConnectionService.class);
    }

    @Test
    public void asProperties() throws Exception {
        Config config = ConfigFactory.empty()
                .withValue("hibernate.query.startup_check", ConfigValueFactory.fromAnyRef(false))
                .withValue("hibernate.format_sql", ConfigValueFactory.fromAnyRef(true));

        Properties properties = ConnectionConfig.asProperties(config, "hibernate");

        assertThat(properties).hasSize(2);
        assertThat(properties.get("hibernate.query.startup_check")).isEqualTo("false");
        assertThat(properties.get("hibernate.format_sql")).isEqualTo("true");
    }
}