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
package io.github.jonestimd.finance.swing.asset;

import java.awt.Window;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.JPanel;
import javax.swing.JTextField;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.dialog.FormDialog;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.validation.RequiredValidator;
import io.github.jonestimd.swing.validation.ValidatedTextField;
import io.github.jonestimd.swing.validation.Validator;
import io.github.jonestimd.text.StringFormat;
import io.github.jonestimd.util.Streams;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.util.JavaFunctions.*;

public class SecurityDialog extends FormDialog {
    private static final String RESOURCE_PREFIX = "dialog.security.";
    private static final SecurityNameValidator<Security> NAME_VALIDATOR = new SecurityNameValidator<>(nullGuard(Security::getName));
    private static final AssetSymbolValidator<Security> SYMBOL_VALIDATOR = new AssetSymbolValidator<>();
    private Security security;
    private JTextField nameField;
    private JTextField symbolField;
    private BeanListComboBox<String> typeField;

    public SecurityDialog(Window owner, final Iterable<Security> securities) {
        super(owner, null, LABELS.get());
        nameField = new ValidatedTextField(value -> NAME_VALIDATOR.validate(security, value, securities));
        nameField.setColumns(30);
        symbolField = new ValidatedTextField(value -> SYMBOL_VALIDATOR.validate(security, value, securities));
        Set<String> types = Streams.of(securities).map(security -> security == null ? null : security.getType())
                .filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));
        Validator<String> typeValidator = new RequiredValidator(LABELS.getString("validation.security.typeRequired"));
        typeField = BeanListComboBox.builder(new StringFormat(), types).editable(typeValidator).get();
        buildForm(getFormPanel());
    }

    private void initializeComponents() {
        setTitle(LABELS.getString(RESOURCE_PREFIX + (security.getId() == null ? "title.new" : "title.edit")));
        nameField.setText(security.getName());
        symbolField.setText(security.getSymbol());
        typeField.getEditor().setItem(security.getType());
    }

    private void buildForm(JPanel formPanel) {
        GridBagBuilder builder = new GridBagBuilder(formPanel, LABELS.get(), RESOURCE_PREFIX);
        builder.append("name.mnemonicAndName", nameField);
        builder.append("symbol.mnemonicAndName", symbolField);
        builder.append("type.mnemonicAndName", typeField);
    }

    public boolean show(Security security) {
        this.security = security;
        initializeComponents();
        pack();
        setVisible(true);
        return ! isCancelled();
    }

    protected void onSave() {
        super.onSave();
        security.setName(nameField.getText());
        security.setSymbol(symbolField.getText());
        security.setType(typeField.getSelectedItem());
    }

    public Security getSecurity() {
        return security;
    }
}
