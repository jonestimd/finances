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

import java.awt.BorderLayout;
import java.awt.Window;
import java.sql.SQLException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.typesafe.config.ConfigRenderOptions;
import io.github.jonestimd.swing.dialog.FormDialog;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class ConnectionDialog extends FormDialog {
    public static final String RESOURCE_PREFIX = "dialog.connection.";
    private final List<DatabaseConfig> driverTemplates;
    private final ConnectionPanel connectionPanel;

    public ConnectionDialog(Window owner, List<DatabaseConfig> driverTemplates) throws SQLException {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), LABELS.get());
        this.driverTemplates = driverTemplates;
        connectionPanel = new ConnectionPanel(driverTemplates, cancelAction);
        getFormPanel().setLayout(new BorderLayout());
        getFormPanel().add(connectionPanel, BorderLayout.CENTER);
        getFormPanel().setBorder(BorderFactory.createEmptyBorder());
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
            List<DatabaseConfig> driverTemplates = DatabaseConfig.loadTemplates();
            ConnectionDialog dialog = new ConnectionDialog(JOptionPane.getRootFrame(), driverTemplates);
            dialog.pack();
            dialog.setVisible(true);

            if (!dialog.isCancelled()) {
                ConfigRenderOptions renderOptions = ConfigRenderOptions.concise().setFormatted(true).setJson(false);
                System.out.println(dialog.connectionPanel.toConfig().root().render(renderOptions));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
