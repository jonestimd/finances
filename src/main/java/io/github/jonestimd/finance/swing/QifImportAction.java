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
package io.github.jonestimd.finance.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.jonestimd.finance.file.quicken.QuickenContext;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.function.MessageConsumer;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.action.MnemonicAction;
import io.github.jonestimd.swing.dialog.ExceptionDialog;
import io.github.jonestimd.swing.window.StatusFrame;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class QifImportAction extends MnemonicAction {
    public static final String ERROR_DIALOG_TITLE = "action.import.qif.failed.title";
    private final ServiceLocator serviceLocator;
    private final JFileChooser fileChooser = new JFileChooser();
    private final FileFilter fileFilter = new FileNameExtensionFilter("Quicken Import Format", "qif", "QIF");

    public QifImportAction(ServiceLocator serviceLocator) {
        super(LABELS.get(), "menu.file.import.qif");
        this.serviceLocator = serviceLocator;
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(fileFilter);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        fileChooser.setFileFilter(fileFilter);
        int result = fileChooser.showDialog(ComponentTreeUtils.findAncestor((Component) event.getSource(), JRootPane.class), null);
        if (result == JFileChooser.APPROVE_OPTION) {
            StatusFrame window = ComponentTreeUtils.findAncestor((JComponent) event.getSource(), StatusFrame.class);
            File selectedFile = fileChooser.getSelectedFile();
            window.disableUI("Reading " + selectedFile.getName() + "...");
            CompletableFuture.runAsync(() -> importFile(selectedFile, window))
                    .whenCompleteAsync((x, ex) -> onComplete(window, ex), SwingUtilities::invokeLater);
        }
    }

    private void importFile(File selectedFile, StatusFrame window) {
        MessageConsumer updateProgress = MessageConsumer.forBundle(MESSAGES.get(), setStatusMessage(window));
        try (FileReader qifReader = new FileReader(selectedFile)) {
            new QuickenContext(serviceLocator).newQifImport().importFile(qifReader, updateProgress);
        } catch (IOException ex) {
            throw new QuickenException("unexpectedException", ex);
        }
    }

    private Consumer<String> setStatusMessage(StatusFrame window) {
        return message -> SwingUtilities.invokeLater(() -> window.setStatusMessage(message));
    }

    private void onComplete(StatusFrame window, Throwable ex) {
        if (ex instanceof CompletionException) ex = ex.getCause();
        if (ex instanceof QuickenException) {
            showQuickenException(window, (QuickenException) ex);
        }
        else if (ex != null) {
            new ExceptionDialog(window, ex).setVisible(true);
        }
        window.enableUI(); // TODO refresh open windows
    }

    private void showQuickenException(StatusFrame window, QuickenException ex) {
        String message = formatMessages(ex);
        if (ex.getRootCause() == null) {
            JOptionPane.showMessageDialog(window, message, LABELS.getString(ERROR_DIALOG_TITLE), JOptionPane.ERROR_MESSAGE);
        }
        else new ExceptionDialog(window, message, ex).setVisible(true);
    }

    private String formatMessages(QuickenException ex) {
        List<String> messages = ex.getMessages();
        if (messages.size() > 1) {
            StringBuilder builder = new StringBuilder("<html>");
            messages.forEach(message -> builder.append(message).append("<br>"));
            return builder.replace(builder.length() - 4, builder.length(), "</html>").toString();
        }
        return messages.get(0);
    }
}
