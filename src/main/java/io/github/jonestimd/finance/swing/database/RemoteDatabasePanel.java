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

import java.util.Arrays;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.text.JTextComponent;

import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.validation.ValidatedPasswordField;
import io.github.jonestimd.swing.validation.ValidatedTextField;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.swing.component.ComponentBinder.*;

public class RemoteDatabasePanel extends JPanel implements ConfigurationPanel {
    private static final String INVALID_PORT = LABELS.getString(RESOURCE_PREFIX + "port.invalid");
    public static final String PASSWORD = "password";
    private static final String PASSWORD_REQUIRED = LABELS.getString(RESOURCE_PREFIX + PASSWORD + REQUIRED_SUFFIX);
    private static final String PASSWORD_MISMATCH = LABELS.getString(RESOURCE_PREFIX + "password.mismatch");
    private static final String NAME_SUFFIX = ".mnemonicAndName";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String SCHEMA = "schema";
    public static final String USER = "user";
    public static final String CONFIRM_PASSWORD = "confirmPassword";

    private DatabaseConfig model;

    private final ValidatedTextField hostField = new ValidatedTextField(validator(requiredValidator(HOST)));
    private final ValidatedTextField portField = new ValidatedTextField(validator(requiredValidator(PORT).then(this::validatePort)));
    private final ValidatedTextField schemaField = new ValidatedTextField(validator(requiredValidator(SCHEMA)));
    private final ValidatedTextField userField = new ValidatedTextField(validator(requiredValidator(USER)));
    private final ValidatedPasswordField passwordField = new ValidatedPasswordField(validator(this::validatePassword));
    private final ValidatedPasswordField confirmField = new ValidatedPasswordField(validator(this::validateConfirmPassword));

    public RemoteDatabasePanel(DatabaseConfig model) {
        GridBagBuilder builder = new GridBagBuilder(this, LABELS.get(), RESOURCE_PREFIX);
        appendField(builder, HOST, bind(hostField, setParameter(HOST)));
        appendField(builder, PORT, bind(portField, this::parsePort, setParameter(PORT)));
        appendField(builder, SCHEMA, bind(schemaField, setParameter(SCHEMA)));
        appendField(builder, USER, bind(userField, setParameter(USER)));
        appendField(builder, PASSWORD, bind(passwordField, setParameter(PASSWORD)));
        appendField(builder, CONFIRM_PASSWORD, confirmField);
        onChange(passwordField, confirmField::validateValue);
        setModel(model);
    }

    @Override
    public void setModel(DatabaseConfig model) {
        this.model = model;
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

    private <V> Consumer<V> setParameter(String name) {
        return value -> model.put(name, value);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        hostField.validateValue();
        portField.validateValue();
        schemaField.validateValue();
        userField.validateValue();
        passwordField.validateValue();
        confirmField.validateValue();
    }

    @Override
    public boolean isSelected() {
        return isVisible();
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

    private String validatePassword(char[] password) {
        return password.length == 0 ? PASSWORD_REQUIRED : null;
    }

    private String validateConfirmPassword(char[] password) {
        return Arrays.equals(password, passwordField.getPassword()) ? null : PASSWORD_MISMATCH;
    }
}
