package io.github.jonestimd.finance.file.quicken.qif;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.operations.PayeeOperations;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

public class PayeeCacheTest {
    private PayeeOperations payeeOperations = mock(PayeeOperations.class);

    private Payee createPayee(String name) {
        Payee payee = new Payee();
        payee.setName(name);
        return payee;
    }

    @Test
    public void testGetAllPayeesUsesCache() throws Exception {
        List<Payee> payees = Arrays.asList(createPayee("payee1"), createPayee("payee2"));
        when(payeeOperations.getAllPayees()).thenReturn(payees);

        PayeeCache payeeCache = new PayeeCache(payeeOperations);
        List<Payee> cachePayees = payeeCache.getAllPayees();
        assertThat(cachePayees).isEqualTo(payeeCache.getAllPayees());

        verify(payeeOperations).getAllPayees();
        verifyNoMoreInteractions(payeeOperations);
        assertThat(payees).containsOnly(cachePayees.toArray());
    }

    @Test
    public void testGetPayeeDoesNotCallDelegate() throws Exception {
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());

        PayeeCache payeeCache = new PayeeCache(payeeOperations);
        assertThat(payeeCache.getPayee("payee1")).isNull();

        verify(payeeOperations).getAllPayees();
        verifyNoMoreInteractions(payeeOperations);
    }

    @Test
    public void testGetPayeeIgnoresCase() throws Exception {
        List<Payee> payees = Arrays.asList(createPayee("payee1"), createPayee("payee2"));
        when(payeeOperations.getAllPayees()).thenReturn(payees);

        PayeeCache payeeCache = new PayeeCache(payeeOperations);
        assertThat("payee1").isEqualTo(payeeCache.getPayee("Payee1").getName());

        verify(payeeOperations).getAllPayees();
        verifyNoMoreInteractions(payeeOperations);
    }

    @Test
    public void testCreatePayeeUpdatesCache() throws Exception {
        Payee payee = createPayee("payee1");
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());
        when(payeeOperations.createPayee("payee1")).thenReturn(payee);
        PayeeCache payeeCache = new PayeeCache(payeeOperations);

        assertThat(payee).isSameAs(payeeCache.createPayee("payee1"));
        assertThat(payee).isSameAs(payeeCache.getPayee("payee1"));

        verify(payeeOperations).getAllPayees();
        verify(payeeOperations).createPayee("payee1");
        verifyNoMoreInteractions(payeeOperations);
    }

    @Test
    public void testMergeRemovesPayeesFromCache() throws Exception {
        Payee payee1 = createPayee("Payee1");
        Payee payee2 = createPayee("Payee2");
        when(payeeOperations.getAllPayees()).thenReturn(Arrays.asList(payee1, payee2));
        when(payeeOperations.merge(anyListOf(Payee.class), any(Payee.class))).thenReturn(Arrays.asList(payee2));
        PayeeCache payeeCache = new PayeeCache(payeeOperations);
        assertThat(payeeCache.getPayee("payee2")).isSameAs(payee2);

        payeeCache.merge(Arrays.asList(payee2), payee1);

        verify(payeeOperations).merge(Arrays.asList(payee2), payee1);
        assertThat(payeeCache.getPayee("payee2")).isNull();
        assertThat(payeeCache.getPayee("payee1")).isSameAs(payee1);
    }
}