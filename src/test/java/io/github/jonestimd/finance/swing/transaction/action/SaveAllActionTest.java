package io.github.jonestimd.finance.swing.transaction.action;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.operations.TransactionBulkUpdate;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.transaction.TransactionTable;
import io.github.jonestimd.finance.swing.transaction.TransactionTableModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SaveAllActionTest {
    @Mock
    private TransactionTable transactionTable;
    @Mock
    private TransactionTableModel tableModel;
    @Mock
    private TransactionService transactionService;
    @Mock
    private DomainEventPublisher domainEventPublisher;
    @InjectMocks
    private SaveAllAction saveAllAction;
    @Captor
    private ArgumentCaptor<TransactionBulkUpdate> updateCaptor;

    private List<DomainEvent<?, ?>> domainEvents = new ArrayList<>();

    @Before
    public void trainMocks() throws Exception {
        when(transactionTable.getModel()).thenReturn(tableModel);
        doReturn(domainEvents).when(transactionService).updateTransactions(any(TransactionBulkUpdate.class));
    }

    @Test
    public void doesNotSaveEmptyUpdate() throws Exception {
        when(tableModel.getChangedRows()).thenAnswer(invocation -> Stream.empty());

        saveAllAction.actionPerformed(null);

        verifyZeroInteractions(transactionService);
    }

    @Test
    public void doesNotSaveNewEmptyTransaction() throws Exception {
        when(tableModel.getChangedRows()).thenAnswer(invocation -> Stream.of(new Transaction()));

        saveAllAction.actionPerformed(null);

        verifyZeroInteractions(transactionService);
    }

    @Test
    public void doesNotSaveInvalidTransaction() throws Exception {
        when(tableModel.getChangedRows()).thenReturn(Stream.of(new TransactionBuilder().detailAmounts((BigDecimal) null).get()));

        saveAllAction.actionPerformed(null);

        verifyZeroInteractions(transactionService);
    }

    @Test
    public void savesValidTransaction() throws Exception {
        final Transaction transaction = new TransactionBuilder().nextId().detailAmounts(BigDecimal.ONE).get();
        when(tableModel.getChangedRows()).thenAnswer(invocation -> Stream.of(transaction));

        saveAllAction.actionPerformed(null);
        SwingUtilities.invokeAndWait(() -> {});

        verify(transactionService).updateTransactions(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getUpdates().get(0).getTransaction()).isSameAs(transaction);
    }
}