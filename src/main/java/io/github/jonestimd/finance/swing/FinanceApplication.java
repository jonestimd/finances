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
package io.github.jonestimd.finance.swing;

import java.awt.Insets;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import io.github.jonestimd.finance.SystemProperty;
import io.github.jonestimd.finance.config.ConfigManager;
import io.github.jonestimd.finance.dao.HibernateDaoContext;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.DriverService;
import io.github.jonestimd.finance.service.ServiceContext;
import io.github.jonestimd.finance.swing.transaction.TransactionsPanel;
import io.github.jonestimd.swing.BackgroundRunner;
import io.github.jonestimd.swing.BackgroundTask;
import io.github.jonestimd.swing.dialog.ExceptionDialog;
import io.github.jonestimd.swing.window.StatusFrame;
import org.apache.log4j.Logger;

public class FinanceApplication {
    private static final String PROPERTIES_FILENAME = ".finances/finances.properties";
    private static final ResourceBundle bundle = BundleType.LABELS.get();
    private static final Logger logger = Logger.getLogger(FinanceApplication.class);

    public static void main(String args[]) {
        if (Boolean.getBoolean("debug-focus")) {
            java.util.logging.Logger.getLogger("java.awt.focus.Component").setLevel(java.util.logging.Level.FINEST);
        }
        try {
            // PlasticLookAndFeel.setPlasticTheme(new DesertBlue());
            // UIManager.installLookAndFeel("JGoodies Plastic XP",
            // PlasticXPLookAndFeel.class.getName());
            // UIManager.setLookAndFeel("com.jgoodies.looks.windows.WindowsLookAndFeel");
            UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
            // UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticLookAndFeel");
            // com.jgoodies.looks.plastic.Plastic3DLookAndFeel
            // com.jgoodies.looks.windows.WindowsLookAndFeel
            // UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            // UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
            // UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");

            // Fix insets for combo box in a table. Required because ComboBoxCellEditor sets JComboBox.isTableCellEditor to false.
            // May need to change if not using JGoodies Plastic Looks
            UIManager.getDefaults().put("ComboBox.editorInsets", new Insets(0, 0, 0, 0));
        }
        catch (Exception ex) {
            logger.warn("Failed to set look and feel", ex);
        }
        UIManager.getDefaults().addResourceBundle(UiOverrideBundle.class.getName());

        logger.info("starting Swing");
        final FinanceApplication instance = new FinanceApplication();
        SwingUtilities.invokeLater(instance::showFrame);

        try {
            SwingUtilities.invokeLater(instance::initializeFrame);
        }
        catch (Exception ex) {
            logger.error("initialization failed", ex);
        }
    }

    private StatusFrame initialFrame;
    private final WindowType initialFrameType;
    private ServiceContext serviceContext;
    private SwingContext swingContext;
    private final Long lastAccountId;

    private FinanceApplication() {
        new PropertiesPersister(PROPERTIES_FILENAME);
        lastAccountId = Long.getLong(SystemProperty.LAST_ACCOUNT.key());
        initialFrameType = lastAccountId == null ? WindowType.ACCOUNTS : WindowType.TRANSACTIONS;
    }

    private void showFrame() {
        initialFrame = new StatusFrame(bundle, initialFrameType.getResourcePrefix());
        initialFrame.setVisible(true);
        initialFrame.disableUI(bundle.getString("configuration.status"));
        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler());
    }

    private Account getLastAccount() {
        return lastAccountId == null ? null : serviceContext.getAccountOperations().getAccount(lastAccountId);
    }

    private void setProgressMessage(String message) {
        SwingUtilities.invokeLater(() -> initialFrame.setStatusMessage(message));
    }

    private void initializeFrame() {
        Optional<DriverService> driverOption = new ConfigManager().loadDriver(initialFrame);
        if (driverOption.isPresent()) {
            new BackgroundRunner<>(BackgroundTask.task(
                    () -> startContext(driverOption.get()),
                    this::showFrameOrError)).doTask();
        }
        else System.exit(1);
    }

    private Exception startContext(DriverService driver) {
        try {
            logger.info("starting context");
            boolean initDatabase = driver.prepareDatabase(this::setProgressMessage);
            serviceContext = new ServiceContext(HibernateDaoContext.connect(initDatabase, driver, this::setProgressMessage));
            swingContext = new SwingContext(serviceContext);
            logger.info("done");
            return null;
        } catch (Exception ex) {
            logger.error("Error starting context", ex);
            return ex;
        }
    }

    private void showFrameOrError(Exception error) {
        if (error == null) {
            if (initialFrameType == WindowType.TRANSACTIONS) {
                TransactionsPanel transactionsPanel = (TransactionsPanel) swingContext.getFrameManager().addFrame(initialFrame, initialFrameType);
                final Account lastAccount = getLastAccount();
                if (lastAccount != null) transactionsPanel.setSelectedAccount(lastAccount);
                else initialFrame.enableUI();
            }
            else {
                swingContext.getFrameManager().addSingletonFrame(initialFrameType, initialFrame);
                initialFrame.enableUI();
            }
        }
        else {
            new ExceptionDialog(initialFrame, error).setVisible(true);
            initialFrame.setVisible(false);
            initialFrame.dispose();
        }
    }

    private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable ex) {
            logger.error("unexpected exception", ex);
            new ExceptionDialog(JOptionPane.getRootFrame(), ex).setVisible(true);
        }
    }
}