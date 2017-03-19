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

import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import com.typesafe.config.Config;
import io.github.jonestimd.finance.swing.FormatFactory;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.layout.FormElement;
import io.github.jonestimd.swing.layout.GridBagBuilder;
import io.github.jonestimd.swing.validation.RequiredValidator;
import io.github.jonestimd.swing.validation.ValidatedComponent;
import io.github.jonestimd.swing.validation.Validator;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class ConnectionPanel extends JPanel {
    public static final String RESOURCE_PREFIX = "database.configuration.";
    public static final String NAME_SUFFIX = ".mnemonicAndName";
    public static final String REQUIRED_SUFFIX = ".required";

    private final BeanListComboBox<DatabaseConfig> driverComboBox;
    private final CardLayout configLayout = new CardLayout();
    private final JPanel configPanel = new JPanel(configLayout);

    public ConnectionPanel(List<DatabaseConfig> driverTemplates, Action cancelAction) {
        configPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED), BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        driverComboBox = new BeanListComboBox<>(FormatFactory.format(DatabaseConfig::getTemplateName), driverTemplates);
        driverComboBox.addItemListener(this::driverSelected);
        driverComboBox.setSelectedIndex(0);

        GridBagBuilder builder = new GridBagBuilder(this, LABELS.get(), RESOURCE_PREFIX);
        builder.append("driver" + NAME_SUFFIX, driverComboBox);
        builder.append(configPanel, FormElement.PANEL);
        for (DatabaseConfig config : driverComboBox.getModel()) {
            if (config.isEmbeddedDatabase()) {
                addPanel(config.getTemplateName(), new EmbeddedDatabasePanel(config, cancelAction));
            }
            else {
                addPanel(config.getTemplateName(), new RemoteDatabasePanel(config));
            }
        }
    }

    public ConnectionPanel(DatabaseConfig config, List<DatabaseConfig> driverTemplates, Action cancelAction) {
        this(driverTemplates, cancelAction);
    }

    private Validator<String> requiredValidator(String field) {
        return new RequiredValidator(LABELS.getString(RESOURCE_PREFIX + field + REQUIRED_SUFFIX));
    }

    private <T extends JComponent & ConfigurationPanel> void addPanel(String name, T panel) {
        configPanel.add(name, panel);
    }

    private void driverSelected(ItemEvent event) {
        if (event.getStateChange() == ItemEvent.SELECTED) {
            String templateName = driverComboBox.getSelectedItem().getTemplateName();
            configLayout.show(configPanel, templateName);
            ComponentTreeUtils.visitComponentTree(configPanel, ValidatedComponent.class, ValidatedComponent::validateValue);
        }
    }

    public Config toConfig() {
        return driverComboBox.getSelectedItem().toConfig("database");
    }
}