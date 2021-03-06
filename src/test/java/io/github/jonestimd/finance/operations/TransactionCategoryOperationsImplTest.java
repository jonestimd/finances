package io.github.jonestimd.finance.operations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import io.github.jonestimd.finance.dao.MockDaoContext;
import io.github.jonestimd.finance.dao.TransactionCategoryDao;
import io.github.jonestimd.finance.dao.TransactionDetailDao;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionCategorySummary;
import io.github.jonestimd.finance.service.ServiceContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TransactionCategoryOperationsImplTest {
    private MockDaoContext daoRepository = new MockDaoContext();
    private ServiceContext serviceContext;
    private TransactionCategoryDao transactionCategoryDao;
    private TransactionDetailDao transactionDetailDao;
    private TransactionCategoryOperations transactionCategoryOperations;

    @Before
    public void resetMocks() throws IOException {
        serviceContext = new ServiceContext(daoRepository);
        transactionCategoryDao = daoRepository.getTransactionCategoryDao();
        transactionDetailDao = daoRepository.getTransactionDetailDao();
        transactionCategoryOperations = serviceContext.getTransactionCategoryOperations();
        daoRepository.resetMocks();
    }

    @Test
    public void testGetTransactionCategoryWithMultipleCodes() throws Exception {
        daoRepository.expectCommit();
        String code = "code";
        String subcode = "subcode";
        TransactionCategory expectedType = new TransactionCategory();
        when(transactionCategoryDao.getTransactionCategory(code, subcode)).thenReturn(expectedType);

        assertThat(transactionCategoryOperations.getTransactionCategory(code, subcode)).isSameAs(expectedType);
    }

    @Test
    public void testGetSecurityAction() throws Exception {
        daoRepository.expectCommit();
        String code = "code";
        TransactionCategory expectedType = new TransactionCategory();
        when(transactionCategoryDao.getSecurityAction(code)).thenReturn(expectedType);

        assertThat(transactionCategoryOperations.getSecurityAction(code)).isSameAs(expectedType);
    }

    @Test
    public void testGetAllTransactionCategorys() throws Exception {
        daoRepository.expectCommit();
        when(transactionCategoryDao.getAll()).thenReturn(Collections.emptyList());

        assertThat(transactionCategoryOperations.getAllTransactionCategories()).isEmpty();
    }

    @Test
    public void testSave() throws Exception {
        daoRepository.expectCommit();
        TransactionCategory type = new TransactionCategory();
        when(transactionCategoryDao.save(type)).thenReturn(type);

        assertThat(transactionCategoryOperations.save(type)).isSameAs(type);

        verify(transactionCategoryDao).save(type);
    }

    @Test
    public void testGetOrCreateTransactionCategorySaves() throws Exception {
        daoRepository.expectCommit();
        String description = "Description";
        String code = "code";
        when(transactionCategoryDao.getTransactionCategory(code)).thenReturn(null);

        transactionCategoryOperations.getOrCreateTransactionCategory(description, true, code);

        ArgumentCaptor<TransactionCategory> capture = ArgumentCaptor.forClass(TransactionCategory.class);
        verify(transactionCategoryDao).save(capture.capture());
        TransactionCategory type = capture.getValue();
        assertThat(type.getDescription()).isEqualTo(description);
        assertThat(type.getCode()).isEqualTo(code);
        assertThat(type.isIncome()).isTrue();
    }

    @Test
    public void testGetOrCreateTransactionCategoryLooksUpParent() throws Exception {
        daoRepository.expectCommit();
        String description = "Description";
        String code = "code";
        String parentCode = "parent";
        TransactionCategory parentType = new TransactionCategory();
        when(transactionCategoryDao.getTransactionCategory(parentCode, code)).thenReturn(null);
        when(transactionCategoryDao.getTransactionCategory(parentCode)).thenReturn(parentType);

        transactionCategoryOperations.getOrCreateTransactionCategory(description, false, parentCode, code);

        ArgumentCaptor<TransactionCategory> capture = ArgumentCaptor.forClass(TransactionCategory.class);
        verify(transactionCategoryDao).save(capture.capture());
        assertThat(capture.getValue().getParent()).isSameAs(parentType);
    }

    @Test
    public void testGetOrCreateTransactionCategoryThrowsExceptionForUnknownParent() throws Exception {
        daoRepository.expectRollback();
        String parentCode = "parent";
        when(transactionCategoryDao.getTransactionCategory(parentCode, "code")).thenReturn(null);
        when(transactionCategoryDao.getTransactionCategory(parentCode)).thenReturn(null);

        try {
            transactionCategoryOperations.getOrCreateTransactionCategory("description", true, parentCode, "code");
            fail("expected an exception");
        }
        catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("Unknown transaction category: " + parentCode + '.');
        }
    }

    @Test
    public void testGetOrCreateTransactionCategoryDoesntSaveDuplicate() throws Exception {
        daoRepository.expectCommit();
        TransactionCategory existingType = new TransactionCategory();
        String code = "child";
        when(transactionCategoryDao.getTransactionCategory(code)).thenReturn(existingType);

        assertThat(transactionCategoryOperations.getOrCreateTransactionCategory("description", true, code)).isSameAs(existingType);
    }

    @Test
    public void testGetTransactionCategorySummaries() throws Exception {
        daoRepository.expectCommit();
        List<TransactionCategorySummary> summaries = singletonList(new TransactionCategorySummary());
        when(transactionCategoryDao.getTransactionCategorySummaries()).thenReturn(summaries);

        assertThat(transactionCategoryOperations.getTransactionCategorySummaries()).isSameAs(summaries);
    }

    @Test
    public void deleteAll() throws Exception {
        daoRepository.expectCommit();
        List<TransactionCategory> categories = new ArrayList<>();

        transactionCategoryOperations.deleteAll(categories);

        verify(transactionCategoryDao).deleteAll(same(categories));
    }

    @Test
    public void mergeDeletesReplacedItems() throws Exception {
        daoRepository.expectCommit();
        List<TransactionCategory> toReplace = Lists.newArrayList(new TransactionCategory(1L));
        TransactionCategory replacement = new TransactionCategory();

        assertThat(transactionCategoryOperations.merge(toReplace, replacement)).isEqualTo(toReplace);

        InOrder inOrder = inOrder(transactionDetailDao, transactionCategoryDao);
        inOrder.verify(transactionDetailDao).replaceCategory(same(toReplace), same(replacement));
        inOrder.verify(transactionCategoryDao).deleteAll(eq(toReplace));
    }

    @Test
    public void mergeDoesNotDeleteCategoriesWithChildren() throws Exception {
        daoRepository.expectCommit();
        TransactionCategory parent = new TransactionCategory(1L);
        TransactionCategory other = new TransactionCategory(2L);
        TransactionCategory replacement = new TransactionCategory(3L);
        List<TransactionCategory> toReplace = Lists.newArrayList(parent, other);
        when(transactionCategoryDao.getParentCategories()).thenReturn(Lists.newArrayList(parent));

        assertThat(transactionCategoryOperations.merge(toReplace, replacement)).isEqualTo(toReplace.subList(1, 2));

        InOrder inOrder = inOrder(transactionDetailDao, transactionCategoryDao);
        inOrder.verify(transactionDetailDao).replaceCategory(same(toReplace), same(replacement));
        inOrder.verify(transactionCategoryDao).deleteAll(toReplace.subList(1, 2));
    }
}