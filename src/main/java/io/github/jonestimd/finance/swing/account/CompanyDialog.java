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
package io.github.jonestimd.finance.swing.account;

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.base.Joiner;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.event.CompanyEvent;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.operations.AccountOperations;
import io.github.jonestimd.finance.swing.BundleType;
import io.github.jonestimd.finance.swing.DomainEventTablePanel;
import io.github.jonestimd.finance.swing.FinanceTableFactory;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.swing.dialog.MessageDialog;
import io.github.jonestimd.util.Streams;

import static org.apache.commons.lang.StringUtils.*;

public class CompanyDialog extends MessageDialog {
    private static final String RESOURCE_PREFIX = "company.dialog.";

    private final String deleteConfirmationMessage;
    private final String deleteConfirmationTitle;
    private final AccountOperations accountOperations;
    private final CompanyTableModel tableModel = new CompanyTableModel();

    public CompanyDialog(Window owner, FinanceTableFactory tableFactory, AccountOperations accountOperations, DomainEventPublisher eventPublisher) {
        super(owner, BundleType.LABELS.getString(RESOURCE_PREFIX + "title"), ModalityType.DOCUMENT_MODAL);
        this.accountOperations = accountOperations;
        deleteConfirmationTitle = BundleType.LABELS.getString(RESOURCE_PREFIX + "confirmation.delete.title");
        deleteConfirmationMessage = BundleType.LABELS.getString(RESOURCE_PREFIX + "confirmation.delete.message");
        setContentPane(new CompanyPanel(tableFactory, eventPublisher));
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            pack();
            setSize(500, 500); // TODO save/retrieve in preferences
            setLocationRelativeTo(getOwner());
        }
        super.setVisible(visible);
    }

    private List<Company> getCompaniesWithAccounts(List<Company> companies) {
        return Streams.filter(companies, Company::nonEmpty);
    }

    private List<Company> checkForAccounts(List<Company> companies) { // TODO check for accounts with same name on companies to be deleted
        List<Company> withAccounts = getCompaniesWithAccounts(companies);
        if (!withAccounts.isEmpty()) {
            int response = JOptionPane.showConfirmDialog(this, String.format(deleteConfirmationMessage,
                    "<li>" + Joiner.on("</li><li>").join(Company.names(withAccounts)) + "</li>"),
                    deleteConfirmationTitle, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (response == JOptionPane.NO_OPTION) {
                companies.removeAll(withAccounts);
            }
            else if (response != JOptionPane.YES_OPTION) {
                companies.clear();
            }
        }
        return companies;
    }

    private List<Company> getDeletes() {
        return tableModel.getPendingDeletes();
    }

    private List<Company> getUpdates() {
        return tableModel.getPendingUpdates().collect(Collectors.toList());
    }

    private class CompanyPanel extends DomainEventTablePanel<Company> {
        public CompanyPanel(FinanceTableFactory tableFactory, DomainEventPublisher eventPublisher) {
            super(eventPublisher, tableFactory.createValidatedTable(tableModel, CompanyTableModel.NAME_INDEX), "company");
        }

        @Override
        public void addNotify() {
            super.addNotify();
            SwingUtilities.invokeLater(getTable()::requestFocusInWindow);
        }

        @Override
        protected boolean isMatch(Company tableRow, String criteria) {
            return criteria.isEmpty() || containsIgnoreCase(tableRow.getName(), criteria);
        }

        @Override
        protected List<? extends DomainEvent<?, ?>> saveChanges(Stream<Company> changedRows, List<Company> deletedRows) {
            List<CompanyEvent> events = new ArrayList<>();
            if (! getDeletes().isEmpty()) {
                accountOperations.deleteCompanies(getDeletes());
                events.add(new CompanyEvent(this, EventType.DELETED, getDeletes()));
            }
            if (! getUpdates().isEmpty()) {
                accountOperations.saveCompanies(getUpdates());
                events.add(new CompanyEvent(this, EventType.CHANGED, getUpdates()));
            }
            return events;
        }

        @Override
        protected Company newBean() {
            Company company = new Company();
            company.setAccounts(new ArrayList<>());
            return company;
        }

        @Override
        protected List<Company> confirmDelete(List<Company> items) {
            return checkForAccounts(items);
        }

        @Override
        protected List<Company> getTableData() {
            return accountOperations.getAllCompanies();
        }
    }
}