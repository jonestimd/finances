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

import java.awt.Component;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.Field;
import io.github.jonestimd.finance.swing.database.SuperUserDialog;
import io.github.jonestimd.jdbc.DriverUtils;
import io.github.jonestimd.swing.validation.ValidatedPasswordField;
import io.github.jonestimd.swing.validation.ValidatedTextField;
import io.github.jonestimd.util.JavaPredicates;
import org.assertj.swing.core.BasicRobot;
import org.assertj.swing.core.ComponentFinder;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.core.Robot;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.github.jonestimd.finance.plugin.DriverConfigurationService.Field.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MysqlDriverConnectionServiceTest {
    private final MySqlDriverConnectionService service = new MySqlDriverConnectionService();
    @Mock
    private Consumer<String> updateProgress;
    @Mock
    private Driver driver;
    @Mock
    private Connection userConnection;
    private List<Connection> superConnections = new ArrayList<>();
    @Mock
    private PreparedStatement dbTestStatement;

    private Map<String, PreparedStatement> preparedStatements = new HashMap<>();
    private List<Statement> statements = new ArrayList<>();

    private Robot robot;

    @Before
    public void setDriver() throws Exception {
        final String url = "jdbc:mysql://localhost:3306/finances";
        final String superUrl = "jdbc:mysql://localhost:3306";
        preparedStatements.clear();
        statements.clear();
        when(driver.acceptsURL(url)).thenReturn(true);
        when(driver.acceptsURL(superUrl)).thenReturn(true);
        when(driver.connect(eq(url), any(Properties.class))).thenReturn(userConnection);
        when(userConnection.prepareStatement(anyString())).thenAnswer(this::addPreparedStatement);
        when(driver.connect(eq(superUrl), any(Properties.class))).thenAnswer(invocation -> {
            Connection superConnection = mock(Connection.class);
            superConnections.add(superConnection);
            when(superConnection.createStatement()).thenAnswer(this::addStatement);
            when(superConnection.prepareStatement(anyString())).thenAnswer(this::addPreparedStatement);
            return superConnection;
        });
        DriverUtils.setDriver(url, driver);
        robot = BasicRobot.robotWithNewAwtHierarchy();
    }

    private PreparedStatement addPreparedStatement(InvocationOnMock invocationOnMock) {
        String query = (String) invocationOnMock.getArguments()[0];
        if (query.equals("select * from company")) return dbTestStatement;
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        preparedStatements.put(query, preparedStatement);
        return preparedStatement;
    }

    private Statement addStatement(InvocationOnMock invocationOnMock) {
        Statement statement = mock(Statement.class);
        statements.add(statement);
        return statement;
    }

    @After
    public void clearDriver() throws Exception {
        DriverManager.deregisterDriver(driver);
        robot.cleanUp();
    }

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
        assertThat(service.isEnabled(USER)).isTrue();
        assertThat(service.isEnabled(Field.PASSWORD)).isTrue();
    }

    @Test
    public void isRequired() throws Exception {
        assertThat(service.isRequired(Field.DIRECTORY)).isFalse();
        assertThat(service.isRequired(Field.HOST)).isTrue();
        assertThat(service.isRequired(Field.PORT)).isTrue();
        assertThat(service.isRequired(Field.SCHEMA)).isTrue();
        assertThat(service.isRequired(USER)).isTrue();
        assertThat(service.isRequired(Field.PASSWORD)).isTrue();
    }

    @Test
    public void getDefaultValues() throws Exception {
        Map<Field, String> values = service.getDefaultValues();

        assertThat(values).hasSize(4);
        assertThat(values.get(Field.HOST)).isEqualTo("localhost");
        assertThat(values.get(Field.PORT)).isEqualTo("3306");
        assertThat(values.get(Field.SCHEMA)).isEqualTo("finances");
        assertThat(values.get(USER)).isEqualTo("finances");
    }

    @Test
    public void getHibernateProperties() throws Exception {
        Config config = ConfigFactory.parseMap(new MapBuilder<String, String>()
                .put(Field.HOST.toString(), "myhost.com")
                .put(Field.PORT.toString(), "1234")
                .put(Field.SCHEMA.toString(), "myschema")
                .put(USER.toString(), "myuser")
                .put(Field.PASSWORD.toString(), "mypassword").get());

        Properties properties = service.getHibernateProperties(config);

        assertThat(properties).hasSize(5);
        assertThat(properties.getProperty("hibernate.dialect")).isEqualTo("org.hibernate.dialect.MySQL5InnoDBDialect");
        assertThat(properties.getProperty("hibernate.connection.driver_class")).isEqualTo("com.mysql.jdbc.Driver");
        assertThat(properties.getProperty("hibernate.connection.url")).isEqualTo("jdbc:mysql://myhost.com:1234/myschema");
        assertThat(properties.getProperty("hibernate.connection.username")).isEqualTo("myuser");
        assertThat(properties.getProperty("hibernate.connection.password")).isEqualTo("mypassword");
    }

    private void verifyTestDatabaseQuery() throws SQLException {
        verify(userConnection).prepareStatement("select * from company");
        verify(dbTestStatement).getMetaData();
    }

    private void verifyPreparedStatement(Connection connection, String query, String... parameters) throws Exception {
        assertThat(preparedStatements).hasSize(1);
        verify(connection).prepareStatement(query);
        for (int i = 0; i < parameters.length; i++) {
            verify(preparedStatements.get(query)).setString(i + 1, parameters[i]);
        }
    }

    private void verifyCloseConnections() throws SQLException {
        verify(userConnection).close();
        for (Connection connection : superConnections) {
            verify(connection).close();
        }
        for (PreparedStatement statement : preparedStatements.values()) {
            verify(statement).close();
        }
        for (Statement statement : statements) {
            verify(statement).close();
        }
    }

    @Test
    public void prepareDatabaseThrowsUnknownException() throws Exception {
        Config config = defaultConfig();
        when(dbTestStatement.getMetaData()).thenThrow(new SQLException("unexpected error"));

        try {
            service.prepareDatabase(config, updateProgress);
            fail("expected an exception");
        } catch (SQLException ex) {
            assertThat(ex.getMessage()).isEqualTo("unexpected error");
        }

        assertThat(preparedStatements).isEmpty();
        assertThat(statements).isEmpty();
        verifyTestDatabaseQuery();
        assertThat(superConnections).isEmpty();
        verify(userConnection).close();
    }

    @Test
    public void unknownDatabaseCreatesSchema() throws Exception {
        Config config = defaultConfig();
        when(dbTestStatement.getMetaData()).thenThrow(new SQLException("unknown database finances"));

        assertThat(service.prepareDatabase(config, updateProgress)).isTrue();

        verifyPreparedStatement(superConnections.get(0), "create user if not exists ?", config.getString(USER.toString()));
        verify(updateProgress).accept("Creating database...");
        verifyTestDatabaseQuery();
        verify(superConnections.get(0)).createStatement();
        verifyCreateDatabase(config);
        verifyCloseConnections();
    }

    @Test
    public void cancelSuperUserDialogThrowsException() throws Exception {
        Config config = defaultConfig();
        SQLException exception = new SQLException("access denied");
        when(dbTestStatement.getMetaData()).thenThrow(exception);
        PrepareDatabaseRunner runner = new PrepareDatabaseRunner(config);

        SwingUtilities.invokeLater(runner);
        cancelSuperUserDialog(robot.finder());

        while (! runner.complete) Thread.yield();
        assertThat(runner.createTables).isFalse();
        assertThat(runner.thrownException).isSameAs(exception);
        verifyTestDatabaseQuery();
    }

    private void cancelSuperUserDialog(ComponentFinder finder) throws Exception {
        SuperUserDialog dialog = finder.findByType(SuperUserDialog.class);
        JButton button = finder.find(dialog, buttonMatcher("Cancel"));
        SwingUtilities.invokeAndWait(button::doClick);
    }

    @Test
    public void accessDeniedCreatesUserWithoutPassword() throws Exception {
        testCreateUserWithoutPassword("Access denied for user 'finances'@'%'");
    }

    @Test
    public void tableDoesntExistCreatesUserWithoutPassword() throws Exception {
        testCreateUserWithoutPassword("Table finances.company' doesn't exist");
    }

    private void testCreateUserWithoutPassword(String errorMessage) throws Exception {
        Config config = defaultConfig();
        when(dbTestStatement.getMetaData()).thenThrow(new SQLException(errorMessage));
        PrepareDatabaseRunner runner = new PrepareDatabaseRunner(config);

        SwingUtilities.invokeLater(runner);
        enterSuperUser(robot.finder());

        while (! runner.complete) Thread.yield();
        if (runner.thrownException != null) runner.thrownException.printStackTrace();
        assertThat(runner.createTables).isTrue();
        assertThat(runner.thrownException).isNull();
        verify(updateProgress).accept("Creating database...");
        verifyTestDatabaseQuery();
        assertThat(superConnections).hasSize(2);
        verify(superConnections.get(0), never()).prepareStatement(anyString());
        verify(superConnections.get(0), never()).createStatement();
        verifyPreparedStatement(superConnections.get(1), "create user if not exists ?", config.getString(USER.toString()));
        verify(superConnections.get(1)).createStatement();
        verifyCreateDatabase(config);
        verifyCloseConnections();
    }

    @Test
    public void accessDeniedCreatesUserWithPassword() throws Exception {
        Config config = defaultConfig().withValue(PASSWORD.toString(), ConfigValueFactory.fromAnyRef("password"));
        when(dbTestStatement.getMetaData()).thenThrow(new SQLException("access denied"));
        PrepareDatabaseRunner runner = new PrepareDatabaseRunner(config);

        SwingUtilities.invokeLater(runner);
        enterSuperUser(robot.finder());

        while (! runner.complete) Thread.yield();
        if (runner.thrownException != null) runner.thrownException.printStackTrace();
        assertThat(runner.createTables).isTrue();
        assertThat(runner.thrownException).isNull();
        verify(updateProgress).accept("Creating database...");
        verifyTestDatabaseQuery();
        assertThat(superConnections).hasSize(2);
        verify(superConnections.get(0), never()).prepareStatement(anyString());
        verify(superConnections.get(0), never()).createStatement();
        verifyPreparedStatement(superConnections.get(1), "create user if not exists ? identified by ?",
                config.getString(USER.toString()), config.getString(PASSWORD.toString()));
        verify(superConnections.get(1)).createStatement();
        verifyCreateDatabase(config);
        verifyCloseConnections();
    }

    private void verifyCreateDatabase(Config config) throws SQLException {
        assertThat(statements).hasSize(1);
        verify(statements.get(0)).execute("create database if not exists " + config.getString(SCHEMA.toString()));
        verify(statements.get(0)).execute("grant all on " + config.getString(SCHEMA.toString()) + ".* to " + config.getString(USER.toString()));
    }

    private void enterSuperUser(ComponentFinder finder) throws Exception {
        SuperUserDialog dialog = finder.findByType(SuperUserDialog.class);
        ValidatedTextField userField = finder.findByType(dialog, ValidatedTextField.class);
        Collection<ValidatedPasswordField> passwordFields = finder.findAll(dialog, matcher(ValidatedPasswordField.class));
        JButton saveButton = finder.find(dialog, buttonMatcher("OK"));
        SwingUtilities.invokeAndWait(() -> {
            userField.setText("superuser");
            for (ValidatedPasswordField field : passwordFields) {
                field.setText("superpassword");
            }
            saveButton.doClick();
        });
    }

    private Config defaultConfig() {
        Properties properties = new Properties();
        service.getDefaultValues().forEach((key, value) -> properties.setProperty(key.toString(), value));
        return ConfigFactory.parseProperties(properties);
    }

    private <T extends Component> GenericTypeMatcher<T> matcher(Class<T> componentType) {
        return matcher(componentType, JavaPredicates.alwaysTrue());
    }

    private GenericTypeMatcher<JButton> buttonMatcher(String text) {
        return matcher(JButton.class, button -> button.getText().equals(text));
    }

    private <T extends Component> GenericTypeMatcher<T> matcher(Class<T> componentType, Predicate<T> condition) {
        return new GenericTypeMatcher<T>(componentType) {
            @Override
            protected boolean isMatching(@Nonnull T component) {
                return condition.test(component);
            }
        };
    }

    private class PrepareDatabaseRunner implements Runnable {
        private final Config config;
        private volatile Exception thrownException;
        private volatile boolean complete = false;
        private volatile boolean createTables;

        private PrepareDatabaseRunner(Config config) {
            this.config = config;
        }

        @Override
        public void run() {
            try {
                createTables = service.prepareDatabase(config, updateProgress);
            } catch (Exception e) {
                thrownException = e;
            } finally {
                complete = true;
            }
        }
    }
}