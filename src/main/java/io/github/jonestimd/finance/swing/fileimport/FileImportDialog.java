// The MIT License (MIT)
//
// Copyright (c) 2018 Tim Jones
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
package io.github.jonestimd.finance.swing.fileimport;

import java.awt.BorderLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.swing.laf.LookAndFeelConfig;
import io.github.jonestimd.swing.dialog.FormDialog;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class FileImportDialog extends FormDialog {
    public static final String RESOURCE_PREFIX = "dialog.fileImport.";
    private final JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
    private final ImportFilePanel filePanel;
    private final ImportFieldsPanel fieldsPanel = new ImportFieldsPanel();

    public FileImportDialog(Window owner, List<Account> accounts, List<Payee> payees) {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), LABELS.get());
        filePanel = new ImportFilePanel(accounts, payees);
        tabbedPane.add("File", filePanel); // TODO label from resources
        getFormPanel().setLayout(new BorderLayout(0, 10));
        getFormPanel().add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.add("Fields", fieldsPanel);
        // TODO mappings
        // TODO filter regex's, negate amount, memo
    }

    public boolean show(ImportFile importFile) {
        // TODO update title
        filePanel.setImportFile(importFile);
        fieldsPanel.setImportFile(importFile);
        pack();
        setVisible(true);
        return false; // TODO check for valid and changed
    }

    public static void main(String... args) {
        LookAndFeelConfig.load();
        new FileImportDialog(JOptionPane.getRootFrame(), new ArrayList<>(), new ArrayList<>()).show(new ImportFile());
    }
}
