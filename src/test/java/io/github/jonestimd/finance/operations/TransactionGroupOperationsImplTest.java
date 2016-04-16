package io.github.jonestimd.finance.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.jonestimd.finance.dao.MockDaoContext;
import io.github.jonestimd.finance.dao.TransactionGroupDao;
import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.service.ServiceContext;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

public class TransactionGroupOperationsImplTest {
    private MockDaoContext daoRepository = new MockDaoContext();
    private TransactionGroupDao transactionGroupDao;
    private TransactionGroupOperations transactionGroupOperations;

    @Before
    public void resetMocks() throws IOException {
        ServiceContext serviceContext = new ServiceContext(daoRepository);
        transactionGroupDao = daoRepository.getTransactionGroupDao();
        transactionGroupOperations = serviceContext.getTransactionGroupOperations();
        daoRepository.resetMocks();
    }

    @Test
    public void testGetAllTransactionGroups() throws Exception {
        daoRepository.expectCommit();
        List<TransactionGroup> groups = new ArrayList<TransactionGroup>();
        when(transactionGroupDao.getAll()).thenReturn(groups);

        assertSame(groups, transactionGroupOperations.getAllTransactionGroups());
    }

    @Test
    public void getOrCreateTransactionGroupCallsDao() throws Exception {
        daoRepository.expectCommit();
        TransactionGroup group = new TransactionGroup();
        when(transactionGroupDao.getTransactionGroup(null)).thenReturn(null);
        when(transactionGroupDao.save(group)).thenReturn(group);

        assertSame(group, transactionGroupOperations.getOrCreateTransactionGroup(group));

        verify(transactionGroupDao).save(group);
    }

    @Test
    public void getTransactionGroupCallsDao() throws Exception {
        daoRepository.expectCommit();
        String groupName = "name";
        TransactionGroup group = new TransactionGroup();
        when(transactionGroupDao.getTransactionGroup(groupName)).thenReturn(group);

        assertSame(group, transactionGroupOperations.getTransactionGroup(groupName));
    }

    @Test
    public void getOrCreateIfUniqueDoesntSaveDuplicate() throws Exception {
        daoRepository.expectCommit();
        TransactionGroup existingGroup = new TransactionGroup();
        TransactionGroup group = new TransactionGroup();
        group.setName("name");
        when(transactionGroupDao.getTransactionGroup(group.getName())).thenReturn(existingGroup);

        assertSame(existingGroup, transactionGroupOperations.getOrCreateTransactionGroup(group));
    }

    @Test
    public void deleteAllCallsDao() throws Exception {
        List<TransactionGroup> groups = new ArrayList<>();

        transactionGroupOperations.deleteAll(groups);

        verify(transactionGroupDao).deleteAll(same(groups));
    }
}