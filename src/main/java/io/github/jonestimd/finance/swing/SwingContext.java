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
package io.github.jonestimd.finance.swing;

import java.awt.Container;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.domain.transaction.TransactionType;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.account.AccountsPanel;
import io.github.jonestimd.finance.swing.account.CompanyFormat;
import io.github.jonestimd.finance.swing.asset.AccountSecuritiesPanel;
import io.github.jonestimd.finance.swing.asset.SecuritiesPanel;
import io.github.jonestimd.finance.swing.asset.SecurityCellEditor;
import io.github.jonestimd.finance.swing.asset.SecurityFormat;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.transaction.AccountFormat;
import io.github.jonestimd.finance.swing.transaction.NotificationIcon;
import io.github.jonestimd.finance.swing.transaction.NotificationIconTableCellRenderer;
import io.github.jonestimd.finance.swing.transaction.PayeeCellEditor;
import io.github.jonestimd.finance.swing.transaction.PayeeFormat;
import io.github.jonestimd.finance.swing.transaction.PayeesPanel;
import io.github.jonestimd.finance.swing.transaction.TransactionCategoriesPanel;
import io.github.jonestimd.finance.swing.transaction.TransactionGroupCellEditor;
import io.github.jonestimd.finance.swing.transaction.TransactionGroupFormat;
import io.github.jonestimd.finance.swing.transaction.TransactionGroupsPanel;
import io.github.jonestimd.finance.swing.transaction.TransactionTypeCellEditor;
import io.github.jonestimd.finance.swing.transaction.TransactionTypeFormat;
import io.github.jonestimd.finance.swing.transaction.TransactionsPanelFactory;
import io.github.jonestimd.swing.table.CurrencyTableCellRenderer;
import io.github.jonestimd.swing.table.DateCellEditor;
import io.github.jonestimd.swing.table.DateTableCellRenderer;
import io.github.jonestimd.swing.table.FormatTableCellRenderer;
import io.github.jonestimd.swing.table.HighlightTableCellRenderer;
import io.github.jonestimd.swing.table.Highlighter;
import io.github.jonestimd.swing.table.TableFactory;
import io.github.jonestimd.swing.table.TableInitializer;
import io.github.jonestimd.swing.window.ApplicationWindowAction;
import io.github.jonestimd.swing.window.FrameManager;
import io.github.jonestimd.swing.window.PanelFactory;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class SwingContext {
    private final ResourceBundle labelBundle = LABELS.get();
    private final ServiceLocator serviceLocator;
    private final DomainEventPublisher domainEventPublisher = new DomainEventPublisher();

    private FinanceTableFactory tableFactory;
    private final PluginContext pluginContext;
    private TransactionsPanelFactory transactionsPanelFactory;
    private final FrameManager<WindowType> frameManager;

    public SwingContext(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
        pluginContext = new PluginContext(serviceLocator, domainEventPublisher);
        TableInitializer tableInitializer = new TableInitializer(defaultTableRenderers(), defaultTableEditors(), columnRenderers(), Collections.emptyMap());
        tableFactory = new FinanceTableFactory(tableInitializer);
        frameManager = new FrameManager<>(BundleType.LABELS.get(), this::getSingletonPanel, buildPanelFactories(), AboutDialog::new);
    }

    private Map<Class<?>, TableCellRenderer> defaultTableRenderers() {
        MapBuilder<Class<?>, TableCellRenderer> builder = new MapBuilder<>();
        pluginContext.getTableCellRenderers().forEach(builder::put);
        return builder.put(Date.class, new DateTableCellRenderer(labelBundle.getString("format.date.pattern")))
                .put(BigDecimal.class, new CurrencyTableCellRenderer(FormatFactory.currencyFormat(), FinanceTableFactory.HIGHLIGHTER))
                .put(Payee.class, new FormatTableCellRenderer(new PayeeFormat(), FinanceTableFactory.HIGHLIGHTER))
                .put(Security.class, new FormatTableCellRenderer(new SecurityFormat(), FinanceTableFactory.HIGHLIGHTER))
                .put(TransactionType.class, new FormatTableCellRenderer(new TransactionTypeFormat(), FinanceTableFactory.HIGHLIGHTER))
                .put(TransactionGroup.class, new FormatTableCellRenderer(new TransactionGroupFormat(), FinanceTableFactory.HIGHLIGHTER))
                .put(Account.class, new FormatTableCellRenderer(new AccountFormat(), FinanceTableFactory.HIGHLIGHTER))
                .put(Company.class, new FormatTableCellRenderer(new CompanyFormat(), FinanceTableFactory.HIGHLIGHTER))
                .put(NotificationIcon.class, new NotificationIconTableCellRenderer())
                .put(String.class, new HighlightTableCellRenderer(FinanceTableFactory.HIGHLIGHTER)).get();
    }

    private Map<Class<?>, Supplier<TableCellEditor>> defaultTableEditors() {
        return new MapBuilder<Class<?>, Supplier<TableCellEditor>>()
                .put(Date.class, () -> new DateCellEditor(labelBundle.getString("format.date.pattern")))
                .put(AccountType.class, () -> TableFactory.createEnumCellEditor(AccountType.class))
                .put(Payee.class, () -> new PayeeCellEditor(serviceLocator, domainEventPublisher))
                .put(Security.class, () -> new SecurityCellEditor(serviceLocator, domainEventPublisher))
                .put(TransactionType.class, () -> new TransactionTypeCellEditor(serviceLocator, domainEventPublisher))
                .put(TransactionGroup.class, () -> new TransactionGroupCellEditor(serviceLocator, domainEventPublisher)).get();
    }

    private Map<String, TableCellRenderer> columnRenderers() {
        MapBuilder<String, TableCellRenderer> builder = new MapBuilder<>();
        pluginContext.getTableColumnRenderers().forEach(builder::put);
        return builder.put("securityShares", new FormatTableCellRenderer(FormatFactory.numberFormat())).get();
    }

    private Container getSingletonPanel(WindowType type) {
        switch (type) {
            case ACCOUNTS:
                return new AccountsPanel(serviceLocator, domainEventPublisher, tableFactory, frameManager);
            case CATEGORIES:
                return new TransactionCategoriesPanel(serviceLocator, domainEventPublisher, tableFactory, frameManager);
            case PAYEES:
                return new PayeesPanel(serviceLocator, domainEventPublisher, tableFactory, frameManager);
            case SECURITIES:
                return new SecuritiesPanel(serviceLocator, domainEventPublisher, pluginContext.getSecurityTableExtensions(), tableFactory, frameManager);
            case ACCOUNT_SECURITIES:
                return new AccountSecuritiesPanel(serviceLocator, domainEventPublisher, pluginContext.getSecurityTableExtensions(), tableFactory, frameManager);
            case TRANSACTION_GROUPS:
                return new TransactionGroupsPanel(serviceLocator, domainEventPublisher, tableFactory, frameManager);
        }
        throw new IllegalArgumentException("invalid window type: " + type);
    }

    private Map<WindowType, PanelFactory<? extends ApplicationWindowAction<WindowType>>> buildPanelFactories() {
        transactionsPanelFactory = new TransactionsPanelFactory(serviceLocator, this, domainEventPublisher);
        return new MapBuilder<WindowType, PanelFactory<? extends ApplicationWindowAction<WindowType>>>()
            .put(WindowType.TRANSACTIONS, transactionsPanelFactory)
            .get();
    }

    public FinanceTableFactory getTableFactory() {
        return tableFactory;
    }

    public FrameManager<WindowType> getFrameManager() {
        return frameManager;
    }
}