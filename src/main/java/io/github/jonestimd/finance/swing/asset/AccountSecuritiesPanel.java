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
package io.github.jonestimd.finance.swing.asset;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.plugin.SecurityTableExtension;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.ReloadEventHandler;
import io.github.jonestimd.finance.swing.event.SingletonWindowEvent;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.action.BackgroundAction;
import io.github.jonestimd.swing.component.MenuActionPanel;
import io.github.jonestimd.swing.table.SectionTable;
import io.github.jonestimd.swing.table.sort.SectionTableRowSorter;
import io.github.jonestimd.swing.window.WindowEventPublisher;

public class AccountSecuritiesPanel extends MenuActionPanel {
    private static final String RESOURCE_GROUP = "accountSecurities";
    public static final String RELOAD_MESSAGE_KEY = "accountSecurities.action.reload.status.initialize";
    private final AssetOperations assetOperations;
    private final WindowEventPublisher<WindowType> windowEventPublisher;
    private final SectionTable<SecuritySummary, AccountSecurityTableModel> table;
    private Action reloadAction;
    @SuppressWarnings("FieldCanBeLocal")
    private final ReloadEventHandler<Long, SecuritySummary> reloadHandler;

    public AccountSecuritiesPanel(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher,
            Iterable<SecurityTableExtension> tableExtensions, FinanceTableFactory tableFactory, WindowEventPublisher<WindowType> windowEventPublisher) {
        this.assetOperations = serviceLocator.getAssetOperations();
        this.windowEventPublisher = windowEventPublisher;
        AccountSecurityTableModel tableModel = new AccountSecurityTableModel(domainEventPublisher, tableExtensions);
        table = tableFactory.createTable(tableModel);
        SectionTableRowSorter<SecuritySummary, AccountSecurityTableModel> sorter = new SectionTableRowSorter<>(table);
        sorter.setRowFilter(SecuritySummary::isNotEmpty);
        sorter.setSortKeys(Lists.newArrayList(new SortKey(AccountSecurityTableModel.NAME_INDEX, SortOrder.ASCENDING)));
        table.setRowSorter(sorter);
        reloadAction = new ReloadActionHandler();
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, new JScrollPane(table));
//        for (SecurityTableExtension extension : tableExtensions) {
//            if (extension instanceof TableSummary) {
//                addSummaries((TableSummary) extension);
//            }
//        }
        reloadHandler = new ReloadEventHandler<>(this, RELOAD_MESSAGE_KEY,
                assetOperations::getSecuritySummariesByAccount, this::getTableModel, SecuritySummary::isSameIds);
        domainEventPublisher.register(SecuritySummary.class, reloadHandler);
    }

    @Override
    protected void initializeMenu(JMenuBar menuBar) {
        menuBar.add(createMenu(), 0);
        menuBar.add(ComponentFactory.newMenuBarSeparator());
        menuBar.add(createToolBar());
        reloadAction.actionPerformed(new ActionEvent(this, -1, null));
    }

    private JMenu createMenu() {
        JMenu menu = ComponentFactory.newMenu(BundleType.LABELS.get(), RESOURCE_GROUP + ".menu.mnemonicAndName");
        menu.add(new JMenuItem(reloadAction));
        return menu;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = ComponentFactory.newMenuToolBar();
        toolBar.add(ComponentFactory.newToolbarButton(SingletonWindowEvent.accountsFrameAction(toolBar, windowEventPublisher)));
        toolBar.add(ComponentFactory.newMenuBarSeparator());
        toolBar.add(ComponentFactory.newToolbarButton(reloadAction));
        return toolBar;
    }

    private AccountSecurityTableModel getTableModel() {
        return table.getModel();
    }

    private class ReloadActionHandler extends BackgroundAction<List<SecuritySummary>> {
        public ReloadActionHandler() {
            super(AccountSecuritiesPanel.this, BundleType.LABELS.get(), RESOURCE_GROUP + ".action.reload");
        }

        @Override
        protected boolean confirmAction(ActionEvent event) {
            return true;
        }

        @Override
        public List<SecuritySummary> performTask() {
            return assetOperations.getSecuritySummariesByAccount();
        }

        @Override
        public void updateUI(List<SecuritySummary> result) {
            table.getModel().setBeans(result);
        }
    }
}
