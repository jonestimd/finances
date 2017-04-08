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

import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class MysqlDriverConnectionServiceTest {
    private final MySqlDriverConnectionService service = new MySqlDriverConnectionService();
    @Mock
    private Consumer<String> updateProgress;

    @Test
    public void getName() throws Exception {
        assertThat(service.getName()).isEqualTo("MySql");
    }

    @Test
    public void isEnabled() throws Exception {
        assertThat(service.isEnabled(Field.DIRECTORY)).isFalse();
        assertThat(service.isEnabled(Field.HOST)).isTrue();
        assertThat(service.isEnabled(Field.PORT)).isTrue();
        assertThat(service.isEnabled(Field.SCHEMA)).isTrue();
        assertThat(service.isEnabled(Field.USER)).isTrue();
        assertThat(service.isEnabled(Field.PASSWORD)).isTrue();
    }

    @Test
    public void isRequired() throws Exception {
        assertThat(service.isRequired(Field.DIRECTORY)).isFalse();
        assertThat(service.isRequired(Field.HOST)).isTrue();
        assertThat(service.isRequired(Field.PORT)).isTrue();
        assertThat(service.isRequired(Field.SCHEMA)).isTrue();
        assertThat(service.isRequired(Field.USER)).isTrue();
        assertThat(service.isRequired(Field.PASSWORD)).isTrue();
    }

    @Test
    public void getDefaultValues() throws Exception {
        Map<Field, String> values = service.getDefaultValues();

        assertThat(values).hasSize(4);
        assertThat(values.get(Field.HOST)).isEqualTo("localhost");
        assertThat(values.get(Field.PORT)).isEqualTo("3306");
        assertThat(values.get(Field.SCHEMA)).isEqualTo("finances");
        assertThat(values.get(Field.USER)).isEqualTo("finances");
    }

    @Test
    public void getHibernateProperties() throws Exception {
        Config config = ConfigFactory.parseMap(new MapBuilder<String, String>()
                .put(Field.HOST.toString(), "myhost.com")
                .put(Field.PORT.toString(), "1234")
                .put(Field.SCHEMA.toString(), "myschema")
                .put(Field.USER.toString(), "myuser")
                .put(Field.PASSWORD.toString(), "mypassword").get());

        Properties properties = service.getHibernateProperties(config);

        assertThat(properties).hasSize(5);
        assertThat(properties.getProperty("hibernate.dialect")).isEqualTo("org.hibernate.dialect.MySQL5InnoDBDialect");
        assertThat(properties.getProperty("hibernate.connection.driver_class")).isEqualTo("com.mysql.jdbc.Driver");
        assertThat(properties.getProperty("hibernate.connection.url")).isEqualTo("jdbc:mysql://myhost.com:1234/myschema");
        assertThat(properties.getProperty("hibernate.connection.username")).isEqualTo("myuser");
        assertThat(properties.getProperty("hibernate.connection.password")).isEqualTo("mypassword");
    }
}