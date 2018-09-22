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

import java.awt.Container;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.google.common.collect.ImmutableMap;
import io.github.jonestimd.collection.MapBuilder;
import io.github.jonestimd.finance.domain.account.Account;
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
import io.github.jonestimd.finance.swing.transaction.TransactionTableModelCache;
import io.github.jonestimd.finance.swing.transaction.TransactionTypeCellEditor;
import io.github.jonestimd.finance.swing.transaction.TransactionTypeFormat;
import io.github.jonestimd.finance.swing.transaction.TransactionsPanelFactory;
import io.github.jonestimd.swing.table.CurrencyTableCellRenderer;
import io.github.jonestimd.swing.table.DateCellEditor;
import io.github.jonestimd.swing.table.DateTableCellRenderer;
import io.github.jonestimd.swing.table.FormatTableCellRenderer;
import io.github.jonestimd.swing.table.HighlightTableCellRenderer;
import io.github.jonestimd.swing.table.Highlighter;
import io.github.jonestimd.swing.table.TableInitializer;
import io.github.jonestimd.swing.window.ApplicationWindowEvent;
import io.github.jonestimd.swing.window.FrameManager;
import io.github.jonestimd.swing.window.PanelFactory;
import io.github.jonestimd.swing.window.WindowEventPublisher;

import static io.github.jonestimd.finance.swing.BundleType.*;

public class SwingContext {
    private final ResourceBundle labelBundle = LABELS.get();
    private final ServiceLocator serviceLocator;
    private final DomainEventPublisher domainEventPublisher = new DomainEventPublisher();
    private final WindowEventPublisher<WindowType> windowEventPublisher = new WindowEventPublisher<>();

    private FinanceTableFactory tableFactory;
    private final PluginContext pluginContext;
    private TransactionsPanelFactory transactionsPanelFactory;
    private final FrameManager<WindowType> frameManager;

    public SwingContext(ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
        pluginContext = new PluginContext(serviceLocator, domainEventPublisher);
        TableInitializer tableInitializer = new TableInitializer(defaultTableRenderers(), defaultTableEditors(), columnRenderers(), Collections.emptyMap());
        tableFactory = new FinanceTableFactory(tableInitializer);
        frameManager = new FrameManager<>(BundleType.LABELS.get(), buildSingletonPanels(), buildPanelFactories(), AboutDialog::new);
        windowEventPublisher.register(frameManager);
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
        return ImmutableMap.of(
                Date.class, () -> new DateCellEditor(labelBundle.getString("format.date.pattern")),
                Payee.class, () -> new PayeeCellEditor(serviceLocator, domainEventPublisher),
                Security.class, () -> new SecurityCellEditor(serviceLocator, domainEventPublisher),
                TransactionType.class, () -> new TransactionTypeCellEditor(serviceLocator, domainEventPublisher),
                TransactionGroup.class, () -> new TransactionGroupCellEditor(serviceLocator, domainEventPublisher));
    }

    private Map<String, TableCellRenderer> columnRenderers() {
        MapBuilder<String, TableCellRenderer> builder = new MapBuilder<>();
        pluginContext.getTableColumnRenderers().forEach(builder::put);
        return builder.put("securityShares", new FormatTableCellRenderer(FormatFactory.numberFormat(), Highlighter.NOOP_HIGHLIGHTER)).get();
    }

    private void buildTransactionsPanelFactory() {
        transactionsPanelFactory = new TransactionsPanelFactory(serviceLocator, new TransactionTableModelCache(domainEventPublisher),
                tableFactory, windowEventPublisher, domainEventPublisher);
    }

    private Map<WindowType, Container> buildSingletonPanels() {
        return ImmutableMap.<WindowType, Container>builder()
            .put(WindowType.ACCOUNTS, new AccountsPanel(serviceLocator, domainEventPublisher, tableFactory, windowEventPublisher))
            .put(WindowType.CATEGORIES, new TransactionCategoriesPanel(serviceLocator, domainEventPublisher, tableFactory, windowEventPublisher))
            .put(WindowType.PAYEES, new PayeesPanel(serviceLocator, domainEventPublisher, tableFactory, windowEventPublisher))
            .put(WindowType.SECURITIES, new SecuritiesPanel(serviceLocator, domainEventPublisher, pluginContext.getSecurityTableExtensions(), tableFactory, windowEventPublisher))
            .put(WindowType.ACCOUNT_SECURITIES, new AccountSecuritiesPanel(serviceLocator, domainEventPublisher, pluginContext.getSecurityTableExtensions(), tableFactory, windowEventPublisher))
            .put(WindowType.TRANSACTION_GROUPS, new TransactionGroupsPanel(serviceLocator, domainEventPublisher, tableFactory, windowEventPublisher))
            .build();
    }

    private Map<WindowType, PanelFactory<? extends ApplicationWindowEvent<WindowType>>> buildPanelFactories() {
        buildTransactionsPanelFactory();
        return ImmutableMap.<WindowType, PanelFactory<? extends ApplicationWindowEvent<WindowType>>>builder()
            .put(WindowType.TRANSACTIONS, transactionsPanelFactory)
            .build();
    }

    public FinanceTableFactory getTableFactory() {
        return tableFactory;
    }

    public FrameManager<WindowType> getFrameManager() {
        return frameManager;
    }
}