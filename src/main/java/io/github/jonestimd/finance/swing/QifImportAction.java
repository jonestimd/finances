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
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.file.ImportSummary;
import io.github.jonestimd.finance.file.quicken.QuickenContext;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.function.MessageConsumer;
import io.github.jonestimd.swing.BackgroundTask;
import io.github.jonestimd.swing.ComponentTreeUtils;
import io.github.jonestimd.swing.action.LocalizedAction;
import io.github.jonestimd.swing.dialog.ExceptionDialog;
import io.github.jonestimd.swing.window.StatusFrame;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class QifImportAction extends LocalizedAction {
    public static final String MESSAGE_PREFIX = "import.qif";
    public static final String RESOURCE_PREFIX = "action." + MESSAGE_PREFIX;
    public static final String ERROR_DIALOG_TITLE = RESOURCE_PREFIX + ".failed.title";
    private final ServiceLocator serviceLocator;
    private final JFileChooser fileChooser = new JFileChooser();
    private final FileFilter fileFilter = new FileNameExtensionFilter("Quicken Import Format", "qif", "QIF");
    private final DomainEventPublisher eventPublisher;

    public QifImportAction(ServiceLocator serviceLocator, DomainEventPublisher eventPublisher) {
        super(LABELS.get(), RESOURCE_PREFIX);
        this.serviceLocator = serviceLocator;
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.addChoosableFileFilter(fileFilter);
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        fileChooser.setFileFilter(fileFilter);
        int result = fileChooser.showDialog(ComponentTreeUtils.findAncestor((Component) event.getSource(), JRootPane.class), null);
        if (result == JFileChooser.APPROVE_OPTION) {
            StatusFrame window = ComponentTreeUtils.findAncestor((JComponent) event.getSource(), StatusFrame.class);
            File selectedFile = fileChooser.getSelectedFile();
            window.disableUI(MESSAGES.formatMessage(MESSAGE_PREFIX + ".start.status", selectedFile.getName()));
            new ImportTask(selectedFile, window).run(window);
        }
    }

    private Consumer<String> setStatusMessage(StatusFrame window) {
        return message -> SwingUtilities.invokeLater(() -> window.setStatusMessage(message));
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

    private class ImportTask extends BackgroundTask<ImportSummary> {
        private final File selectedFile;
        private final StatusFrame window;

        private ImportTask(File selectedFile, StatusFrame window) {
            this.selectedFile = selectedFile;
            this.window = window;
        }

        @Override
        public String getStatusMessage() {
            return MESSAGES.formatMessage(MESSAGE_PREFIX + ".start.status", selectedFile.getName());
        }

        @Override
        public ImportSummary performTask() {
            MessageConsumer updateProgress = MessageConsumer.forBundle(MESSAGES.get(), setStatusMessage(window));
            try (FileReader qifReader = new FileReader(selectedFile)) {
                return new QuickenContext(serviceLocator).newQifImport().importFile(qifReader, updateProgress);
            } catch (IOException ex) {
                throw new QuickenException("unexpectedException", ex);
            }
        }

        @Override
        public void updateUI(ImportSummary summary) {
            JOptionPane.showMessageDialog(window, MESSAGES.formatMessage(MESSAGE_PREFIX + ".summary", summary.getImported(), summary.getIgnored()));
            eventPublisher.publishEvent(new DomainEvent<>(this));
        }

        @Override
        public boolean handleException(Throwable ex) {
            if (ex instanceof QuickenException) {
                showQuickenException(window, (QuickenException) ex);
                return true;
            }
            return false;
        }
    }
}
