package io.github.jonestimd.finance.swing.transaction;

import java.util.Collections;

import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class TransactionTableRowSorterTest {
    @Test
    public void newTransactionAlwaysLast() throws Exception {
        TransactionTableModel model = new TransactionTableModel(new Account(1L));
        TransactionTable table = new TransactionTable(model);
        model.addBean(newTransaction(1L));
        model.addBean(newTransaction(null));
        model.addBean(newTransaction(2L));
        TransactionTableRowSorter sorter = new TransactionTableRowSorter(table);
        sorter.setSortKeys(Collections.singletonList(new SortKey(1, SortOrder.ASCENDING)));

        assertThat(sorter.convertRowIndexToView(0)).isEqualTo(0);
        assertThat(sorter.convertRowIndexToView(1)).isEqualTo(1);
        assertThat(sorter.convertRowIndexToView(4)).isEqualTo(2);
        assertThat(sorter.convertRowIndexToView(5)).isEqualTo(3);
        assertThat(sorter.convertRowIndexToView(2)).isEqualTo(4);
        assertThat(sorter.convertRowIndexToView(3)).isEqualTo(5);

        sorter.toggleSortOrder(1);

        assertThat(sorter.convertRowIndexToView(0)).isEqualTo(2);
        assertThat(sorter.convertRowIndexToView(1)).isEqualTo(3);
        assertThat(sorter.convertRowIndexToView(4)).isEqualTo(0);
        assertThat(sorter.convertRowIndexToView(5)).isEqualTo(1);
        assertThat(sorter.convertRowIndexToView(2)).isEqualTo(4);
        assertThat(sorter.convertRowIndexToView(3)).isEqualTo(5);
    }

    @Test
    public void defaultSortPutsNewDetailAtEndOfTransaction() throws Exception {
        TransactionTableModel model = new TransactionTableModel(new Account(1L));
        Transaction transaction = newTransaction(1L);
        model.addBean(newTransaction(2L));
        model.addBean(transaction);
        model.addBean(newTransaction(null));
        TransactionTable table = new TransactionTable(model);
        RowSorter<? extends TableModel> sorter = table.getRowSorter();

        model.queueAppendSubRow(model.getLeadRowForGroup(1));

        assertThat(sorter.convertRowIndexToView(model.getLeadRowForGroup(1) + transaction.getDetails().size()))
                .isEqualTo(model.getLeadRowForGroup(1) + transaction.getDetails().size());
    }

    private Transaction newTransaction(Long id) {
        Transaction transaction;
        if (id == null) {
            transaction = new Transaction();
        }
        else {
            transaction = new Transaction(id);
            transaction.setNumber(id.toString());
        }
        transaction.addDetails(new TransactionDetail());
        return transaction;
    }
}
