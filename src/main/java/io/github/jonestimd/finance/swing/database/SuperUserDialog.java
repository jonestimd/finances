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

import java.awt.Window;
import java.util.Arrays;

import javax.swing.JLabel;

import io.github.jonestimd.finance.plugin.DriverConfigurationService.Field;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.validation.RequiredValidator;
import io.github.jonestimd.swing.validation.ValidatedPasswordField;
import io.github.jonestimd.swing.validation.ValidatedTextField;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.ResourceKey.*;
import static io.github.jonestimd.swing.component.ComponentBinder.onChange;

public class SuperUserDialog extends FormDialog {
    private static final String RESOURCE_PREFIX = "database.configuration.";
    private static final String PASSWORD_MISMATCH = LABELS.getString(RESOURCE_PREFIX + "password.mismatch");

    private final ValidatedTextField userField = new ValidatedTextField(new RequiredValidator(LABELS.getString(RESOURCE_PREFIX + "user.required")));
    private final ValidatedPasswordField passwordField = new ValidatedPasswordField(LABELS.getString(RESOURCE_PREFIX + "password.required"));

    public SuperUserDialog(Window owner, String message) {
        super(owner, LABELS.getString("dialog.connection.title"), LABELS.get());
        ValidatedPasswordField confirmField = new ValidatedPasswordField(this::validateConfirmPassword);
        GridBagBuilder builder = new GridBagBuilder(getFormPanel(), LABELS.get(), RESOURCE_PREFIX);
        builder.append(new JLabel(message));
        builder.unrelatedVerticalGap();
        builder.append(new JLabel(LABELS.getString(RESOURCE_PREFIX + "superuser.message")));
        builder.append(LABEL.key(Field.USER), userField);
        builder.append(LABEL.key(Field.PASSWORD), passwordField);
        builder.append(LABEL.key("confirmPassword"), confirmField);
        onChange(passwordField, confirmField::validateValue);
    }

    public String getUser() {
        return userField.getText();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    private String validateConfirmPassword(char[] password) {
        return !Arrays.equals(password, passwordField.getPassword()) ? PASSWORD_MISMATCH : null;
    }
}
