// The MIT License (MIT)
//
// Copyright (c) 2019 Tim Jones
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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import io.github.jonestimd.swing.action.MnemonicAction;
import io.github.jonestimd.swing.component.FileSuggestField;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class PdfPreviewDialog extends JDialog {
    public static final String RESOURCE_PREFIX = "dialog.pdfPreview.";

    private final PdfPanel previewPanel;
    private final FileSuggestField fileField;
    private final JButton loadFileButton;
    private final JFormattedTextField scaleField = new JFormattedTextField(DecimalFormat.getIntegerInstance());

    public PdfPreviewDialog(Window owner, PageRegionTableModel regionTableModel) {
        super(owner, LABELS.getString(RESOURCE_PREFIX + "title"), ModalityType.MODELESS);
        previewPanel = new PdfPanel(regionTableModel);
        try {
            fileField = new FileSuggestField(false, new File("."));
            fileField.getEditor().getEditorComponent().addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    fileField.setPopupVisible(false);
                }
            });
            loadFileButton = new JButton(new MnemonicAction(LABELS.getString(RESOURCE_PREFIX + "action.view.mnemonicAndName")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    File file = fileField.getSelectedItem();
                    if (file != null && file.exists() && file.isFile()) previewPanel.setDocument(file);
                }
            });
            scaleField.setMinimumSize(new Dimension(45, scaleField.getPreferredSize().height));
            scaleField.setPreferredSize(new Dimension(45, scaleField.getPreferredSize().height));
            scaleField.setMaximumSize(new Dimension(45, scaleField.getPreferredSize().height));
            scaleField.setValue(100);
            scaleField.addActionListener(event -> previewPanel.setScale((Long) scaleField.getValue()/100f));
            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(new JScrollPane(previewPanel), BorderLayout.CENTER);
            JPanel filePanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets.right = 5;
            gbc.gridy = 0;
            gbc.weightx = 1f;
            gbc.fill = GridBagConstraints.BOTH;
            filePanel.add(fileField, gbc);
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            filePanel.add(loadFileButton, gbc);
            filePanel.add(scaleField, gbc);
            filePanel.add(new JLabel("%"), gbc);
            filePanel.setBorder(new EmptyBorder(5, 5, 5, 0));
            getContentPane().add(filePanel, BorderLayout.SOUTH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
