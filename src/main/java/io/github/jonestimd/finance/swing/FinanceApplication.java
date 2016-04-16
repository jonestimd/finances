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
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import io.github.jonestimd.finance.SystemProperty;
import io.github.jonestimd.finance.dao.HibernateDaoContext;
import io.github.jonestimd.finance.dao.SchemaBuilder;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.service.ServiceContext;
import io.github.jonestimd.finance.swing.transaction.TransactionsPanel;
import io.github.jonestimd.swing.dialog.ExceptionDialog;
import io.github.jonestimd.swing.window.StatusFrame;
import io.github.jonestimd.util.PropertiesLoader;
import org.apache.log4j.Logger;

public class FinanceApplication {
    public static final File DEFAULT_CONNECTION_PROPERTIES = new File(System.getProperty("user.home"), ".finances/connection.properties");
    private static final String PROPERTIES_FILENAME = ".finances/finances.properties";
    private static final ResourceBundle bundle = BundleType.LABELS.get();
    private static final Logger logger = Logger.getLogger(FinanceApplication.class);

    public static void main(String args[]) {
//        java.util.logging.Logger.getLogger("java.awt.focus.Component").setLevel(java.util.logging.Level.FINEST);
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
            instance.start();
        }
        catch (Exception ex) {
            logger.error("initialization failed", ex);
        }
    }

    private StatusFrame initialFrame;
    private ServiceContext serviceContext;
    private SwingContext swingContext;

    private FinanceApplication() {
        new PropertiesPersister(PROPERTIES_FILENAME);
    }

    private void showFrame() {
        initialFrame = new StatusFrame(bundle, WindowType.TRANSACTIONS.getResourcePrefix());
        initialFrame.setVisible(true);
        initialFrame.disableUI(bundle.getString("configuration.status"));
        Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler());
    }

    private void start() throws IOException, SQLException {
        logger.info("starting context");
        serviceContext = new ServiceContext(connect(Boolean.getBoolean("init-database")));
        swingContext = new SwingContext(serviceContext);
        logger.info("done");

        final Account lastAccount = getLastAccount();
        SwingUtilities.invokeLater(() -> initializeFrame(lastAccount));
    }

    public static HibernateDaoContext connect(boolean createSchema) throws IOException, SQLException {
        HibernateDaoContext daoContext = new HibernateDaoContext(loadConnectionProperties());
        if (createSchema) {
            new SchemaBuilder(daoContext).createSchemaTables().seedReferenceData();
        }
        return daoContext;
    }

    public static Properties loadConnectionProperties() throws IOException {
        return PropertiesLoader.load("connection.properties", DEFAULT_CONNECTION_PROPERTIES);
    }

    private Account getLastAccount() {
        Long accountId = Long.getLong(SystemProperty.LAST_ACCOUNT.key());
        return accountId == null ? null : serviceContext.getAccountOperations().getAccount(accountId);
    }

    private void initializeFrame(Account lastAccount) {
        try {
            TransactionsPanel transactionsPanel = (TransactionsPanel) swingContext.getFrameManager().addFrame(initialFrame, WindowType.TRANSACTIONS);
            if (lastAccount != null) {
                transactionsPanel.setSelectedAccount(lastAccount);
            }
            else {
                initialFrame.enableUI();
            }
        }
        catch (Throwable ex) {
            logger.error("Initialization failed", ex);
            new ExceptionDialog(initialFrame, bundle, "configuration", ex).setVisible(true);
        }
    }

    private static class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        public void uncaughtException(Thread t, Throwable ex) {
            logger.error("unexpected exception", ex);
            new ExceptionDialog(JOptionPane.getRootFrame(), ex).setVisible(true);
        }
    }
}