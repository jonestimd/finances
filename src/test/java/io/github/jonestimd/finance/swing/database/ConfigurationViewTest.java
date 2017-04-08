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
package io.github.jonestimd.finance.swing.database;

import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.typesafe.config.Config;
import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.plugin.DriverConfigurationService;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.Field;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.FileSuggestField;
import io.github.jonestimd.swing.validation.ValidatedPasswordField;
import io.github.jonestimd.swing.validation.ValidatedTextField;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static io.github.jonestimd.finance.plugin.DriverConfigurationService.Field.*;
import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.database.ConfigurationView.*;
import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationViewTest {
    public static final String REQUIRED = ".required";
    public static final String INVALID = ".invalid";
    public static final String MISMATCH = ".mismatch";
    @Mock
    private DriverConfigurationService service1;
    @Mock
    private DriverConfigurationService service2;

    @Test
    public void showsAvailableDriverTypes() throws Exception {
        JPanel panel = new JPanel();
        List<DriverConfigurationService> services = Arrays.asList(service1, service2);

        new ConfigurationView(panel, services);

        BeanListComboBox<?> driverComboBox = getDriverComboBox(panel);
        assertThat(driverComboBox.getModel()).containsOnly(service1, service2);
        assertThat(driverComboBox.getSelectedItem()).isSameAs(service1);
        verify(service1).getName();
        verify(service2).getName();
    }

    @Test
    public void enablesFieldsBasedOnDriver() throws Exception {
        JPanel panel = new JPanel();
        when(service1.isEnabled(DIRECTORY)).thenReturn(true);
        when(service2.isEnabled(not(DIRECTORY))).thenReturn(true);
        List<DriverConfigurationService> services = Arrays.asList(service1, service2);

        new ConfigurationView(panel, services);

        assertThat(find(panel, DIRECTORY).isEnabled()).isTrue();
        assertThat(((ValidatedTextField) find(panel, HOST)).isEditable()).isFalse();
        assertThat(((ValidatedTextField) find(panel, PORT)).isEditable()).isFalse();
        assertThat(((ValidatedTextField) find(panel, SCHEMA)).isEditable()).isFalse();
        assertThat(((ValidatedTextField) find(panel, USER)).isEditable()).isFalse();
        assertThat(((ValidatedPasswordField) find(panel, PASSWORD)).isEditable()).isFalse();
        assertThat(((ValidatedPasswordField) find(panel, "Confirm Password")).isEditable()).isFalse();

        getDriverComboBox(panel).setSelectedIndex(1);

        assertThat(find(panel, DIRECTORY).isEnabled()).isFalse();
        assertThat(((ValidatedTextField) find(panel, HOST)).isEditable()).isTrue();
        assertThat(((ValidatedTextField) find(panel, PORT)).isEditable()).isTrue();
        assertThat(((ValidatedTextField) find(panel, SCHEMA)).isEditable()).isTrue();
        assertThat(((ValidatedTextField) find(panel, USER)).isEditable()).isTrue();
        assertThat(((ValidatedPasswordField) find(panel, PASSWORD)).isEditable()).isTrue();
        assertThat(((ValidatedPasswordField) find(panel, "Confirm Password")).isEditable()).isTrue();
    }

    @Test
    public void requiresFieldsBasedOnDriver() throws Exception {
        JPanel panel = new JPanel();
        when(service1.isEnabled(DIRECTORY)).thenReturn(true);
        when(service1.isRequired(DIRECTORY)).thenReturn(true);
        when(service2.isEnabled(not(DIRECTORY))).thenReturn(true);
        when(service2.isRequired(HOST)).thenReturn(true);
        List<DriverConfigurationService> services = Arrays.asList(service1, service2);

        new ConfigurationView(panel, services);

        assertThat(((FileSuggestField) find(panel, DIRECTORY)).getValidationMessages()).isEqualTo(label(DIRECTORY, REQUIRED));
        assertThat(((ValidatedTextField) find(panel, HOST)).getValidationMessages()).isNull();

        getDriverComboBox(panel).setSelectedIndex(1);

        assertThat(((FileSuggestField) find(panel, DIRECTORY)).getValidationMessages()).isNull();
        assertThat(((ValidatedTextField) find(panel, HOST)).getValidationMessages()).isEqualTo(label(HOST, REQUIRED));
    }

    @Test
    public void validatesPort() throws Exception {
        validatesPort(true, "", REQUIRED);
        validatesPort(true, "a", INVALID);
        validatesPort(true, "-1", INVALID);
        validatesPort(true, "0", null);
        validatesPort(true, "65536", INVALID);
        validatesPort(true, "65535", null);

        validatesPort(false, "", null);
        validatesPort(false, "a", INVALID);
        validatesPort(false, "-1", INVALID);
        validatesPort(false, "0", null);
        validatesPort(false, "65536", INVALID);
        validatesPort(false, "65535", null);
    }

    private void validatesPort(boolean required, String port, String validation) {
        JPanel panel = new JPanel();
        when(service1.isEnabled(PORT)).thenReturn(true);
        when(service1.isRequired(PORT)).thenReturn(required);
        List<DriverConfigurationService> services = Arrays.asList(service1, service2);
        new ConfigurationView(panel, services);
        ValidatedTextField field = (ValidatedTextField) find(panel, PORT);

        field.setText(port);

        assertThat(field.getValidationMessages()).isEqualTo(label(PORT, validation));
    }

    @Test
    public void validatesPasswords() throws Exception {
        validatesPasswords(true, null, null, REQUIRED, null);
        validatesPasswords(true, "abc", null, null, MISMATCH);
        validatesPasswords(true, null, "abc", REQUIRED, MISMATCH);
        validatesPasswords(true, "abc", "abc", null, null);

        validatesPasswords(false, "abc", null, null, MISMATCH);
        validatesPasswords(false, null, "abc", null, MISMATCH);
        validatesPasswords(false, "abc", "abc", null, null);
    }

    private void validatesPasswords(boolean required, String password, String confirm, String passwordValidation, String confirmValidation) {
        reset(service1, service2);
        JPanel panel = new JPanel();
        when(service1.isEnabled(PASSWORD)).thenReturn(true);
        when(service1.isRequired(PASSWORD)).thenReturn(required);
        List<DriverConfigurationService> services = Arrays.asList(service1, service2);
        new ConfigurationView(panel, services);
        ValidatedPasswordField passwordField = (ValidatedPasswordField) find(panel, PASSWORD);
        ValidatedPasswordField confirmField  = (ValidatedPasswordField) find(panel, "Confirm Password");

        passwordField.setText(password);
        passwordField.validateValue();
        confirmField.setText(confirm);
        confirmField.validateValue();

        assertThat(passwordField.getValidationMessages()).isEqualTo(label(PASSWORD, passwordValidation));
        assertThat(confirmField.getValidationMessages()).isEqualTo(label(PASSWORD, confirmValidation));
    }

    @Test
    public void setsDefaultsBasedOnDriver() throws Exception {
        JPanel panel = new JPanel();
        Map<Field, String> values = new MapBuilder<Field, String>()
                .put(DIRECTORY, "/home/user")
                .put(HOST, "host name")
                .put(PORT, "1234")
                .put(SCHEMA, "schama name")
                .put(USER, "user ID")
                .put(PASSWORD, "ignored")
                .get();
        when(service1.getDefaultValues()).thenReturn(values);
        when(service1.isEnabled(any())).thenReturn(false);
        List<DriverConfigurationService> services = Arrays.asList(service1, service2);

        new ConfigurationView(panel, services).getDefaultsAction().actionPerformed(null);

        assertThat(((FileSuggestField) find(panel, DIRECTORY)).getSelectedItem().getAbsolutePath()).isEqualTo(values.get(DIRECTORY));
        assertThat(((ValidatedTextField) find(panel, HOST)).getText()).isEqualTo(values.get(HOST));
        assertThat(((ValidatedTextField) find(panel, PORT)).getText()).isEqualTo(values.get(PORT));
        assertThat(((ValidatedTextField) find(panel, SCHEMA)).getText()).isEqualTo(values.get(SCHEMA));
        assertThat(((ValidatedTextField) find(panel, USER)).getText()).isEqualTo(values.get(USER));
        assertThat(((ValidatedPasswordField) find(panel, PASSWORD)).getPassword()).isEmpty();
    }

    @Test
    public void getConfigForEmbededDatabase() throws Exception {
        JPanel panel = new JPanel();
        when(service1.getName()).thenReturn("driver name");
        when(service1.isEnabled(DIRECTORY)).thenReturn(true);
        List<DriverConfigurationService> services = Arrays.asList(service1, service2);
        ConfigurationView view = new ConfigurationView(panel, services);
        ((FileSuggestField) find(panel, DIRECTORY)).setSelectedItem("/user/home");
        ((ValidatedTextField) find(panel, HOST)).setText("host name");
        ((ValidatedTextField) find(panel, PORT)).setText("1234");
        ((ValidatedTextField) find(panel, SCHEMA)).setText("schema name");
        ((ValidatedTextField) find(panel, USER)).setText("user ID");
        ((ValidatedPasswordField) find(panel, PASSWORD)).setText("p@ssw0rd");

        Config config = view.getConfig();

        assertThat(config.getString(DRIVER.toString())).isEqualTo("driver name");
        assertThat(config.getString(DIRECTORY.toString())).isEqualTo("/user/home");
        assertThat(config.hasPath(HOST.toString())).isFalse();
        assertThat(config.hasPath(PORT.toString())).isFalse();
        assertThat(config.hasPath(SCHEMA.toString())).isFalse();
        assertThat(config.hasPath(USER.toString())).isFalse();
        assertThat(config.hasPath(PASSWORD.toString())).isFalse();
    }

    @Test
    public void getConfigForRemoteDatabase() throws Exception {
        JPanel panel = new JPanel();
        when(service1.getName()).thenReturn("driver name");
        when(service1.isEnabled(not(DIRECTORY))).thenReturn(true);
        List<DriverConfigurationService> services = Arrays.asList(service1, service2);
        ConfigurationView view = new ConfigurationView(panel, services);
        ((FileSuggestField) find(panel, DIRECTORY)).setSelectedItem("/user/home");
        ((ValidatedTextField) find(panel, HOST)).setText("host name");
        ((ValidatedTextField) find(panel, PORT)).setText("1234");
        ((ValidatedTextField) find(panel, SCHEMA)).setText("schema name");
        ((ValidatedTextField) find(panel, USER)).setText("user ID");
        ((ValidatedPasswordField) find(panel, PASSWORD)).setText("p@ssw0rd");

        Config config = view.getConfig();

        assertThat(config.getString(DRIVER.toString())).isEqualTo("driver name");
        assertThat(config.hasPath(DIRECTORY.toString())).isFalse();
        assertThat(config.getString(HOST.toString())).isEqualTo("host name");
        assertThat(config.getString(PORT.toString())).isEqualTo("1234");
        assertThat(config.getString(SCHEMA.toString())).isEqualTo("schema name");
        assertThat(config.getString(USER.toString())).isEqualTo("user ID");
        assertThat(config.getString(PASSWORD.toString())).isEqualTo("p@ssw0rd");
    }

    private String label(Field field, String suffix) {
        return suffix == null ? null : LABELS.getString(RESOURCE_PREFIX + field.toString() + suffix);
    }

    private BeanListComboBox<?> getDriverComboBox(JPanel panel) {
        return (BeanListComboBox<?>) find(panel, "Database Type");
    }

    private Component find(JPanel panel, Field field) {
        return find(panel, label(field, ".mnemonicAndName").substring(1));
    }

    private Component find(JPanel panel, String label) {
        for (int i = 0; i < panel.getComponentCount(); i+=2) {
            if (((JLabel) panel.getComponent(i)).getText().equals(label)) {
                return panel.getComponent(i+1);
            }
        }
        return null;
    }

    private static Field not(Field item) {
        return Mockito.argThat(new ArgumentMatcher<Field>() {
            @Override
            @SuppressWarnings("unchecked")
            public boolean matches(Object obj) {
                return obj != item;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("not " + item.name());
            }
        });
    }
}