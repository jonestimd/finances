package io.github.jonestimd.finance.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.jonestimd.finance.dao.PayeeDao;
import io.github.jonestimd.finance.dao.TransactionDao;
import io.github.jonestimd.finance.domain.transaction.Payee;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.same;

public class PayeeOperationsImplTest {
    private PayeeDao payeeDao = mock(PayeeDao.class);
    private TransactionDao transactionDao = mock(TransactionDao.class);
    private PayeeOperationsImpl payeeOperations = new PayeeOperationsImpl(payeeDao, transactionDao);

    @Test
    public void testGetPayeeCallsDao() throws Exception {
        String name = "Payee";
        Payee expectedPayee = new Payee();
        when(payeeDao.getPayee(name)).thenReturn(expectedPayee);

        assertThat(payeeOperations.getPayee(name)).isSameAs(expectedPayee);
    }

    @Test
    public void testCreatePayeeCallsDao() throws Exception {
        String payeeName = "name";
        Payee expectedPayee = new Payee();
        when(payeeDao.save(any(Payee.class))).thenReturn(expectedPayee);

        assertThat(payeeOperations.createPayee(payeeName)).isSameAs(expectedPayee);

        ArgumentCaptor<Payee> capture = ArgumentCaptor.forClass(Payee.class);
        verify(payeeDao).save(capture.capture());
        assertThat(capture.getValue().getName()).isEqualTo(payeeName);
    }

    @Test
    public void deleteAllCallsDao() throws Exception {
        List<Payee> payees = new ArrayList<>();

        payeeOperations.deleteAll(payees);

        verify(payeeDao).deleteAll(same(payees));
    }

    @Test
    public void testMergePayees() throws Exception {
        List<Payee> toReplace = Arrays.asList(new Payee(), new Payee());
        Payee payee = new Payee();

        payeeOperations.merge(toReplace, payee);

        InOrder inOrder = inOrder(payeeDao, transactionDao);
        inOrder.verify(transactionDao).replacePayee(toReplace, payee);
        inOrder.verify(payeeDao).deleteAll(toReplace);
    }
}