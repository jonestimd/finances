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

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.Field;
import io.github.jonestimd.jdbc.DriverUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.github.jonestimd.finance.plugin.DriverConfigurationService.Field.*;
import static java.util.Collections.*;
import static org.fest.assertions.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DerbyDriverConnectionServiceTest {
    private final DerbyDriverConnectionService service = new DerbyDriverConnectionService();
    @Mock
    private Consumer<String> updateProgress;
    @Mock
    private Driver driver;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;

    private final File output = new File("./build/prepareDatabase/finances");
    private final String url = "jdbc:derby:directory:" + output;

    @Before
    public void setDriver() throws Exception {
        when(driver.acceptsURL(url)).thenReturn(true);
        when(driver.connect(any(String.class), any(Properties.class))).thenReturn(connection);
        DriverUtils.setDriver(url, driver);
    }

    @After
    public void clearDriver() throws Exception {
        DriverManager.deregisterDriver(driver);
    }

    @Test
    public void getName() throws Exception {
        assertThat(service.getName()).isEqualTo("Derby");
    }

    @Test
    public void isEnabled() throws Exception {
        assertThat(service.isEnabled(DIRECTORY)).isTrue();
        assertThat(service.isEnabled(Field.HOST)).isFalse();
        assertThat(service.isEnabled(Field.PORT)).isFalse();
        assertThat(service.isEnabled(Field.SCHEMA)).isFalse();
        assertThat(service.isEnabled(Field.USER)).isFalse();
        assertThat(service.isEnabled(Field.PASSWORD)).isFalse();
    }

    @Test
    public void isRequired() throws Exception {
        assertThat(service.isRequired(DIRECTORY)).isTrue();
        assertThat(service.isRequired(Field.HOST)).isFalse();
        assertThat(service.isRequired(Field.PORT)).isFalse();
        assertThat(service.isRequired(Field.SCHEMA)).isFalse();
        assertThat(service.isRequired(Field.USER)).isFalse();
        assertThat(service.isRequired(Field.PASSWORD)).isFalse();
    }

    @Test
    public void getDefaultValues() throws Exception {
        Map<Field, String> values = service.getDefaultValues();

        assertThat(values).hasSize(1);
        assertThat(values.get(DIRECTORY)).isEqualTo(String.join(File.separator, DerbyDriverConnectionService.DEFAULT_PATH));
    }

    @Test
    public void getConnectionProperties() throws Exception {
        Config config = ConfigFactory.parseMap(new MapBuilder<String, String>()
                .put("directory", "/home/user/finances").get());

        Properties properties = service.getConnectionProperties(config);

        assertThat(properties).hasSize(4);
        assertThat(properties.getProperty("hibernate.dialect")).isEqualTo("org.hibernate.dialect.DerbyTenSevenDialect");
        assertThat(properties.getProperty("hibernate.connection.driver_class")).isEqualTo("org.apache.derby.jdbc.EmbeddedDriver");
        assertThat(properties.getProperty("hibernate.connection.url")).isEqualTo("jdbc:derby:directory:/home/user/finances");
        assertThat(properties.getProperty("hibernate.temp.use_jdbc_metadata_defaults")).isEqualTo("false");
    }

    @Test
    public void prepareDatabaseCreatesParentFile() throws Exception {
        deleteAll(output.getParentFile());
        Config config = ConfigFactory.parseMap(singletonMap(DIRECTORY.toString(), output.toString()));

        assertThat(service.prepareDatabase(config, updateProgress)).isTrue();

        assertThat(output).doesNotExist();
        assertThat(output.getParentFile()).exists();
        assertThat(output.getParentFile()).isDirectory();
    }

    @Test
    public void prepareDatabaseDeletesEmptyDirectory() throws Exception {
        deleteAll(output);
        output.mkdirs();
        Config config = ConfigFactory.parseMap(singletonMap(DIRECTORY.toString(), output.toString()));

        assertThat(service.prepareDatabase(config, updateProgress)).isTrue();

        assertThat(output).doesNotExist();
        assertThat(output.getParentFile()).exists();
        assertThat(output.getParentFile()).isDirectory();
    }

    @Test
    public void prepareDatabaseKeepsNonemptyDirectory() throws Exception {
        new File(output, "log").mkdirs();
        Config config = ConfigFactory.parseMap(singletonMap(DIRECTORY.toString(), output.toString()));
        when(connection.prepareStatement("select * from company")).thenReturn(statement);
        when(statement.getMetaData()).thenThrow(new SQLException());

        assertThat(service.prepareDatabase(config, updateProgress)).isTrue();

        assertThat(output).exists();
        assertThat(output).isDirectory();
    }

    @Test
    public void prepareDatabaseKeepsTestsForTables() throws Exception {
        new File(output, "log").mkdirs();
        Config config = ConfigFactory.parseMap(singletonMap(DIRECTORY.toString(), output.toString()));
        when(connection.prepareStatement("select * from company")).thenReturn(statement);
        when(statement.getMetaData()).thenReturn(null);

        assertThat(service.prepareDatabase(config, updateProgress)).isFalse();

        assertThat(output).exists();
        assertThat(output).isDirectory();
    }

    private boolean deleteAll(File file) {
        if (file.isDirectory()) return Stream.of(file.listFiles()).allMatch(this::deleteAll) && file.delete();
        else return file.delete();
    }
}