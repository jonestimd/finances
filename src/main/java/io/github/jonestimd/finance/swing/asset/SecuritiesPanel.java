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
package io.github.jonestimd.finance.swing.asset;

import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;

import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.SecurityEvent;
import io.github.jonestimd.finance.operations.AssetOperations;
import io.github.jonestimd.finance.plugin.SecurityTableExtension;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.WindowType;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.finance.swing.event.ReloadEventHandler;
import io.github.jonestimd.finance.swing.transaction.AccountAccessPanel;
import io.github.jonestimd.swing.ComponentFactory;
import io.github.jonestimd.swing.action.LocalizedAction;
import io.github.jonestimd.swing.component.BeanListComboBox;
import io.github.jonestimd.swing.component.BeanListComboBoxModel;
import io.github.jonestimd.swing.component.ComboBoxCellEditor;
import io.github.jonestimd.swing.table.TableSummary;
import io.github.jonestimd.swing.validation.RequiredValidator;
import io.github.jonestimd.swing.validation.Validator;
import io.github.jonestimd.swing.window.FrameManager;
import io.github.jonestimd.text.StringFormat;

import static io.github.jonestimd.finance.swing.BundleType.*;
import static io.github.jonestimd.finance.swing.asset.SecurityTableModel.*;
import static org.apache.commons.lang.StringUtils.*;

public class SecuritiesPanel extends AccountAccessPanel<Security, SecuritySummary> {
    private final AssetOperations assetOperations;
    private final FinanceTableFactory tableFactory;
    private final BeanListComboBoxModel<String> typesModel = new BeanListComboBoxModel<>();
    private final SplitsDialogAction splitsAction = new SplitsDialogAction();
    private final Action hideZeroSharesAction = new LocalizedAction(BundleType.LABELS.get(), "action.hideZeroShares") {
        @Override
        public void actionPerformed(ActionEvent e) {
            getRowSorter().allRowsChanged();
        }
    };
    @SuppressWarnings("FieldCanBeLocal")
    private final ReloadEventHandler<Long, SecuritySummary> reloadHandler =
            new ReloadEventHandler<>(this, "security.action.reload.status.initialize", this::getTableData, this::getTableModel);

    public SecuritiesPanel(ServiceLocator serviceLocator, DomainEventPublisher domainEventPublisher,
            Iterable<SecurityTableExtension> tableExtensions, FinanceTableFactory tableFactory, FrameManager<WindowType> frameManager) {
        super(domainEventPublisher, tableFactory.validatedTableBuilder(new SecurityTableModel(domainEventPublisher, tableExtensions)).sortedBy(NAME_INDEX).get(), "security", frameManager);
        this.assetOperations = serviceLocator.getAssetOperations();
        this.tableFactory = tableFactory;

        Validator<String> typeValidator = new RequiredValidator(LABELS.getString("validation.security.typeRequired"));
        getTable().getColumn(SecurityColumnAdapter.TYPE_ADAPTER).setCellEditor(new ComboBoxCellEditor(new BeanListComboBox<>(new StringFormat(), typeValidator, typesModel)));
        getTableModel().addTableModelListener(event -> updateTypes());

        for (SecurityTableExtension extension : tableExtensions) {
            if (extension instanceof TableSummary) {
                addSummaries((TableSummary) extension);
            }
        }
        hideZeroSharesAction.putValue(Action.SELECTED_KEY, Boolean.TRUE);
        domainEventPublisher.register(SecuritySummary.class, reloadHandler);
    }

    private void updateTypes() {
        Set<String> types = getTableModel().getBeans().stream()
                .map(summary -> summary == null ? null : summary.getSecurity().getType())
                .filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));
        typesModel.setElements(types, false);
    }

    @Override
    protected boolean isMatch(SecuritySummary tableRow, String criteria) {
        return includeRow(tableRow) && isMatch(tableRow.getSecurity(), criteria);
    }

    private boolean isMatch(Security security, String criteria) {
        return criteria.isEmpty() || containsIgnoreCase(security.getName(), criteria) || containsIgnoreCase(security.getSymbol(), criteria);
    }

    private boolean includeRow(SecuritySummary summary) {
        return ! Boolean.TRUE.equals(hideZeroSharesAction.getValue(Action.SELECTED_KEY)) || summary.getShares().compareTo(BigDecimal.ZERO) != 0;
    }

    @Override
    protected void addActions(JToolBar toolbar) {
        super.addActions(toolbar);
        toolbar.add(ComponentFactory.newToolbarButton(splitsAction));
        toolbar.add(ComponentFactory.newToolbarToggleButton(hideZeroSharesAction));
    }

    @Override
    protected void addActions(JMenu menu) {
        super.addActions(menu);
        menu.add(new JMenuItem(splitsAction));
    }

    @Override
    protected List<SecuritySummary> getTableData() {
        return assetOperations.getSecuritySummaries();
    }

    @Override
    protected SecuritySummary newBean() {
        return new SecuritySummary();
    }

    @Override
    protected void tableSelectionChanged() {
        super.tableSelectionChanged();
        splitsAction.setEnabled(isSingleRowSelected());
    }

    @Override
    protected List<? extends DomainEvent<?, ?>> saveChanges(List<Security> changedSecurities, List<Security> deletedSecurities) {
        List<SecurityEvent> events = new ArrayList<>();
        if (!changedSecurities.isEmpty()) {
            assetOperations.saveAll(changedSecurities);
            events.add(new SecurityEvent(this, EventType.CHANGED, changedSecurities));
        }
        if (!deletedSecurities.isEmpty()) {
            assetOperations.deleteAll(deletedSecurities);
            events.add(new SecurityEvent(this, EventType.DELETED, deletedSecurities));
        }
        return events;
    }

    public class SplitsDialogAction extends LocalizedAction {
        public SplitsDialogAction() {
            super(BundleType.LABELS.get(), "action.stockSplits.edit");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFrame owner = (JFrame) getTopLevelAncestor();
            StockSplitDialog dialog = new StockSplitDialog(owner, tableFactory, getSelectedBean().getSecurity(), assetOperations, eventPublisher);
            dialog.setVisible(true);
        }
    }
}