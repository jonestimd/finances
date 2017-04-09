package io.github.jonestimd.finance.swing.transaction.action;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.DefaultListSelectionModel;
import javax.swing.ListSelectionModel;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.event.TransactionEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionDetailBuilder;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.SelectionDialog;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import io.github.jonestimd.mockito.MockitoHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.github.jonestimd.finance.domain.account.AccountType.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MoveActionTest {
    @Mock
    private TransactionTable table;
    @Mock
    private TransactionTableModel tableModel;
    private ListSelectionModel selectionModel = new DefaultListSelectionModel();
    @Mock
    private TransactionService transactionService;
    @Mock
    private DomainEventPublisher domainEventPublisher;
    @Mock
    private Supplier<Stream<Account>> accountsSupplier;
    @Mock
    private SelectionDialog<Account> dialog;

    private MoveAction moveAction;

    @Before
    public void createAction() throws Exception {
        when(table.getModel()).thenReturn(tableModel);
        when(table.getSelectionModel()).thenReturn(selectionModel);
        moveAction = new MoveAction(table, transactionService, domainEventPublisher, accountsSupplier, dialog);
    }

    @Test
    public void disabledForNewTransaction() throws Exception {
        when(table.getSelectedRowCount()).thenReturn(1);
        when(table.getSelectedTransaction()).thenReturn(new Transaction());

        selectionModel.setSelectionInterval(1, 1);

        assertThat(moveAction.isEnabled()).isFalse();
    }

    @Test
    public void disabledForMultipleRowSelection() throws Exception {
        when(table.getSelectedRowCount()).thenReturn(2);
        when(table.getSelectedTransaction()).thenReturn(new Transaction(1L));

        selectionModel.setSelectionInterval(1, 1);

        assertThat(moveAction.isEnabled()).isFalse();
    }

    @Test
    public void enabledForSinglePersistedTransaction() throws Exception {
        when(table.getSelectedRowCount()).thenReturn(1);
        when(table.getSelectedTransaction()).thenReturn(new Transaction(1L));

        selectionModel.setSelectionInterval(1, 1);

        assertThat(moveAction.isEnabled()).isTrue();
    }

    @Test
    public void accountListDoesNotIncludeCurrentAccount() throws Exception {
        final Account currentAccount = newAccount(BANK);
        when(tableModel.getAccount()).thenReturn(currentAccount);
        when(table.getSelectedTransaction()).thenReturn(new Transaction());
        when(accountsSupplier.get()).thenReturn(Lists.newArrayList(currentAccount, newAccount(BANK), newAccount(BROKERAGE)).stream());

        moveAction.displayDialog(null);

        ArgumentCaptor<List<Account>> accountsCaptor = MockitoHelper.captor();
        verify(dialog).show(accountsCaptor.capture());
        assertThat(accountsCaptor.getValue()).hasSize(2);
        assertThat(accountsCaptor.getValue().contains(currentAccount)).isFalse();
    }

    @Test
    public void onlyAllowMovingSecurityTransactionToSecurityAccount() throws Exception {
        final Account currentAccount = newAccount(BROKERAGE);
        when(tableModel.getAccount()).thenReturn(currentAccount);
        when(table.getSelectedTransaction()).thenReturn(new TransactionBuilder().details(new TransactionDetailBuilder().get()).security(new Security()).get());
        when(accountsSupplier.get()).thenReturn(Lists.newArrayList(currentAccount, newAccount(BANK), newAccount(BROKERAGE), newAccount(_401K)).stream());

        moveAction.displayDialog(null);

        ArgumentCaptor<List<Account>> accountsCaptor = MockitoHelper.captor();
        verify(dialog).show(accountsCaptor.capture());
        assertThat(accountsCaptor.getValue()).hasSize(2);
        assertThat(accountsCaptor.getValue().stream().allMatch(account -> account.getType().isSecurity())).isTrue();
    }

    @Test
    public void notifyDomainEventPublisher() throws Exception {
        final Account selectedAccount = newAccount(BANK);
        final Transaction selectedTransaction = new Transaction();
        final TransactionEvent transactionEvent = new TransactionEvent(this, null, selectedTransaction);
        when(table.getSelectedTransaction()).thenReturn(selectedTransaction);
        when(dialog.getSelectedItem()).thenReturn(selectedAccount);
        doReturn(Lists.newArrayList(transactionEvent)).when(transactionService).moveTransaction(selectedTransaction, selectedAccount);

        moveAction.saveDialogData();
        moveAction.setSaveResultOnUI();

        verify(transactionService).moveTransaction(selectedTransaction, selectedAccount);
        verify(domainEventPublisher).publishEvent(transactionEvent);
    }

    protected Account newAccount(AccountType type) {
        return new AccountBuilder().nextId().type(type).get();
    }
}