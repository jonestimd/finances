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

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.jonestimd.finance.plugin.DriverConfigurationService;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.Field;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.swing.action.ActionAdapter;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.FileSuggestField;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.validation.RequiredValidator;
import io.github.jonestimd.swing.validation.ValidatedPasswordField;
import io.github.jonestimd.swing.validation.ValidatedTextField;
import io.github.jonestimd.swing.validation.Validator;

import static io.github.jonestimd.finance.plugin.DriverConfigurationService.Field.*;
import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.swing.component.ComponentBinder.*;

public class ConfigurationView {
    public static String RESOURCE_PREFIX = "database.configuration.";
    public static String REQUIRED_SUFFIX = ".required";
    private static final String DIRECTORY_REQUIRED = LABELS.getString(RESOURCE_PREFIX + DIRECTORY + REQUIRED_SUFFIX);
    private static final String INVALID_PORT = LABELS.getString(RESOURCE_PREFIX + "port.invalid");
    private static final String PASSWORD_REQUIRED = LABELS.getString(RESOURCE_PREFIX + PASSWORD + REQUIRED_SUFFIX);
    private static final String PASSWORD_MISMATCH = LABELS.getString(RESOURCE_PREFIX + "password.mismatch");
    private static final String NAME_SUFFIX = ".mnemonicAndName";
    public static final String CONFIRM_PASSWORD = "confirmPassword";

    private Map<Field, String> model;

    private final BeanListComboBox<DriverConfigurationService> driverComboBox;
    private final FileSuggestField directoryField = new FileSuggestField(true, new File(System.getProperty("user.home")), DIRECTORY_REQUIRED);
    private final ValidatedTextField hostField;
    private final ValidatedTextField portField;
    private final ValidatedTextField schemaField;
    private final ValidatedTextField userField;
    private final ValidatedPasswordField passwordField;
    private final ValidatedPasswordField confirmField;

    private final Action defaultsAction;

    public ConfigurationView(JPanel panel, List<DriverConfigurationService> driverTemplates) {
        driverComboBox = new BeanListComboBox<>(FormatFactory.format(DriverConfigurationService::getName), driverTemplates);
        driverComboBox.addItemListener(this::driverSelected);

        directoryField.setSelectedItem(null);

        hostField = new ValidatedTextField(validator(HOST));
        hostField.setColumns(LABELS.getInt(RESOURCE_PREFIX + "textColumns"));
        portField = new ValidatedTextField(validator(PORT, this::validatePort));
        schemaField = new ValidatedTextField(validator(SCHEMA));
        userField = new ValidatedTextField(validator(USER));
        passwordField = new ValidatedPasswordField(this::validatePassword);
        confirmField = new ValidatedPasswordField(this::validateConfirmPassword);

        driverComboBox.setSelectedIndex(0);

        GridBagBuilder builder = new GridBagBuilder(panel, LABELS.get(), RESOURCE_PREFIX);
        builder.append("driver" + NAME_SUFFIX, driverComboBox);
        builder.append(DIRECTORY + NAME_SUFFIX, directoryField).addItemListener(this::selectDirectory);
        appendField(builder, HOST.toString(), bind(hostField, setParameter(HOST)));
        appendField(builder, PORT.toString(), bind(portField, this::parsePort, setParameter(PORT)));
        appendField(builder, SCHEMA.toString(), bind(schemaField, setParameter(SCHEMA)));
        appendField(builder, USER.toString(), bind(userField, setParameter(USER)));
        appendField(builder, PASSWORD.toString(), bind(passwordField, setParameter(PASSWORD)));
        appendField(builder, CONFIRM_PASSWORD, confirmField);
        onChange(passwordField, confirmField::validateValue);
        setModel(new HashMap<>());

        this.defaultsAction = ActionAdapter.forMnemonicAndName(this::setDefaults,
                LABELS.getString(RESOURCE_PREFIX + "action.defaults.mnemonicAndName"));
    }

    private void setDefaults(ActionEvent event) {
        Map<Field, String> properties = driverComboBox.getSelectedItem().getDefaultValues();
        properties.entrySet().forEach(entry -> {
            switch (entry.getKey()) {
                case DIRECTORY: directoryField.setSelectedItem(new File(entry.getValue())); break; // TODO update suggestion dropdown
                case HOST: hostField.setText(entry.getValue()); break;
                case PORT: portField.setText(entry.getValue()); break;
                case SCHEMA: schemaField.setText(entry.getValue()); break;
                case USER: userField.setText(entry.getValue()); break;
            }
        });
    }

    private Validator<String> validator(Field field) {
        return requiredValidator(field).when(() -> isRequired(field));
    }

    private Validator<String> validator(Field field, Validator<String> validator) {
        return requiredValidator(field).then(validator).when(() -> isRequired(field));
    }

    private RequiredValidator requiredValidator(Field field) {
        return new RequiredValidator(LABELS.getString(RESOURCE_PREFIX + field + REQUIRED_SUFFIX));
    }

    private void driverSelected(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            DriverConfigurationService plugin = driverComboBox.getSelectedItem();
            directoryField.setEnabled(plugin.isEnabled(DIRECTORY));
            hostField.setEditable(plugin.isEnabled(HOST));
            portField.setEditable(plugin.isEnabled(PORT));
            schemaField.setEditable(plugin.isEnabled(SCHEMA));
            userField.setEditable(plugin.isEnabled(USER));
            passwordField.setEditable(plugin.isEnabled(PASSWORD));
            confirmField.setEditable(plugin.isEnabled(PASSWORD));
        }
    }

    public Action getDefaultsAction() {
        return defaultsAction;
    }

    public void setModel(HashMap<Field, String> model) {
        this.model = model;
        String directory = model.get(DIRECTORY);
        if (directory != null) directoryField.setSelectedItem(new File(directory));
        hostField.setText(model.get(HOST));
        portField.setText(model.get(PORT));
        schemaField.setText(model.get(SCHEMA));
        userField.setText(model.get(USER));
        passwordField.setText(model.get(PASSWORD));
        confirmField.setText(model.get(PASSWORD));
    }

    private void appendField(GridBagBuilder builder, String parameter, JTextComponent field) {
        builder.append(parameter + NAME_SUFFIX, field);
    }

    private void selectDirectory(ItemEvent event) {
        if (event.getItem() != null) model.put(DIRECTORY, event.getItem().toString());
    }

    private <V> Consumer<V> setParameter(Field field) {
        return value -> {
            if (value != null) model.put(field, value.toString());
        };
    }

    private Integer parsePort(String port) {
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String validatePort(String value) {
        Integer port = parsePort(value);
        return port == null || port < 0 || port > 0xffff ? INVALID_PORT : null;
    }

    private boolean isRequired(Field field) {
        DriverConfigurationService configuration = driverComboBox.getSelectedItem();
        return configuration != null && configuration.isRequired(field);
    }

    private String validatePassword(char[] password) {
        return isRequired(PASSWORD) && password.length == 0 ? PASSWORD_REQUIRED : null;
    }

    private String validateConfirmPassword(char[] password) {
        return isRequired(PASSWORD) && ! Arrays.equals(password, passwordField.getPassword()) ? PASSWORD_MISMATCH : null;
    }

    public Config toConfig(String prefix) {
        DriverConfigurationService configuration = driverComboBox.getSelectedItem();
        return configuration == null ? null : toConfig(prefix, configuration);
    }

    private Config toConfig(String prefix, DriverConfigurationService configuration) {
        Properties properties = new Properties();
        model.entrySet().forEach(entry -> {
            if (configuration.isEnabled(entry.getKey())) properties.setProperty(entry.getKey().toString(), entry.getValue());
        });
        return ConfigFactory.empty().withValue(prefix, ConfigFactory.parseProperties(properties).root());
    }
}
