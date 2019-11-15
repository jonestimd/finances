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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.DriverService;
import io.github.jonestimd.jdbc.DriverUtils;
import io.github.jonestimd.util.Streams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DriverConfigurationServiceTest {
    private final String url = "jdbc:test://myhost.com:1234/myschema";
    private final TestDriverConfigurationService testService = new TestDriverConfigurationService();
    @Mock
    private Consumer<String> updateProgress;
    @Mock
    private Driver driver;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;

    @Before
    public void setDriver() throws Exception {
        lenient().when(driver.acceptsURL(url)).thenReturn(true);
        when(driver.connect(any(String.class), any(Properties.class))).thenReturn(connection);
        DriverUtils.setDriver(url, driver);
    }

    @After
    public void removeDriver() throws Exception {
        DriverManager.deregisterDriver(driver);
    }

    @Test
    public void getServices() throws Exception {
        List<DriverConfigurationService> services = DriverConfigurationService.getServices();

        assertThat(Streams.map(services, DriverConfigurationService::getName)).contains("Derby", "MySql", "PostgreSQL");
    }

    @Test
    public void forConfigMatchesDriverName() throws Exception {
        Config config = ConfigFactory.parseMap(new MapBuilder<String, String>()
                .put("driver", "Derby")
                .put("directory", "/home/user/finances").get());

        DriverService service = DriverConfigurationService.forConfig(config);

        Properties properties = service.getHibernateProperties();
        assertThat(properties.getProperty("hibernate.dialect")).isEqualTo("org.hibernate.dialect.DerbyTenSevenDialect");
    }

    @Test
    public void getHibernateProperties() throws Exception {
        Config config = ConfigFactory.empty()
                .withValue("hibernate.query.startup_check", ConfigValueFactory.fromAnyRef(false))
                .withValue("hibernate.format_sql", ConfigValueFactory.fromAnyRef(true));

        Properties properties = testService.getHibernateProperties(config);

        assertThat(properties).hasSize(2);
        assertThat(properties.get("hibernate.query.startup_check")).isEqualTo("false");
        assertThat(properties.get("hibernate.format_sql")).isEqualTo("true");
    }

    @Test
    public void getHibernateResourcesReturnsEmptyList() throws Exception {
        assertThat(testService.getHibernateResources()).isEmpty();
    }

    @Test
    public void prepareDatabaseReturnsFalseForSuccessfulQuery() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        Config config = ConfigFactory.empty();

        assertThat(testService.prepareDatabase(config, updateProgress)).isFalse();

        verify(connection).prepareStatement("select * from company");
        verify(connection).close();
        verify(statement).getMetaData();
        verifyNoInteractions(updateProgress);
    }

    @Test
    public void prepareDatabaseThrowsExceptionForUnsuccessfulQuery() throws Exception {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.getMetaData()).thenThrow(new SQLException("test exception"));
        Config config = ConfigFactory.empty();

        try {
            assertThat(testService.prepareDatabase(config, updateProgress)).isTrue();
            fail("expected an exception");
        } catch (SQLException ex) {
            assertThat(ex.getMessage()).isEqualTo("test exception");
        }

        verify(connection).prepareStatement("select * from company");
        verify(connection).close();
        verify(statement).getMetaData();
        verifyNoInteractions(updateProgress);
    }

    private class TestDriverConfigurationService extends DriverConfigurationService {
        protected TestDriverConfigurationService() {
            super("TestDriver", "TestDialect", DriverConfigurationServiceTest.class.getName(), "test://");
        }

        @Override
        public boolean isEnabled(Field field) {
            return field != Field.DIRECTORY;
        }

        @Override
        public boolean isRequired(Field field) {
            return false;
        }

        @Override
        public Map<Field, String> getDefaultValues() {
            return Collections.emptyMap();
        }

        @Override
        protected String getJdbcUrl(Config config) {
            return url;
        }

        @Override
        public String testConnection(Config config) {
            return null;
        }
    }
}