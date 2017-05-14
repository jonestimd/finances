package io.github.jonestimd.finance.operations;

import java.math.BigDecimal;
import java.util.List;

import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionBuilder;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import org.junit.Test;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

public class TransactionUpdateTest {
    @Test
    public void getNewCategoriesReturnsCategoryWithNullId() throws Exception {
        TransactionCategory parent = new TransactionCategory(-1L);
        TransactionCategory child = new TransactionCategory(parent, "child");
        TransactionDetail detail1 = new TransactionDetail(child, BigDecimal.ONE, null, null);
        TransactionDetail detail2 = new TransactionDetail(parent, BigDecimal.ONE, null, null);
        TransactionDetail detail3 = new TransactionDetail(null, BigDecimal.ONE, null, null);
        Transaction transaction = new TransactionBuilder().details(detail1, detail2, detail3).get();

        List<TransactionCategory> categories = new TransactionUpdate(transaction).getNewCategories();

        assertThat(categories).containsExactly(child);
    }

    @Test
    public void getNewCategoriesCachesResult() throws Exception {
        TransactionCategory parent = new TransactionCategory(-1L);
        TransactionCategory child = new TransactionCategory(parent, "child");
        TransactionDetail detail1 = new TransactionDetail(child, BigDecimal.ONE, null, null);
        TransactionDetail detail2 = new TransactionDetail(parent, BigDecimal.ONE, null, null);
        TransactionDetail detail3 = new TransactionDetail(null, BigDecimal.ONE, null, null);
        Transaction transaction = new TransactionBuilder().details(detail1, detail2, detail3).get();

        TransactionUpdate transactionUpdate = new TransactionUpdate(transaction);
        List<TransactionCategory> categories = transactionUpdate.getNewCategories();

        assertThat(transactionUpdate.getNewCategories()).isSameAs(categories);
    }

    @Test
    public void getNewCategoriesIgnoresDeletedDetails() throws Exception {
        TransactionCategory parent = new TransactionCategory(-1L);
        TransactionCategory child = new TransactionCategory(parent, "child");
        TransactionDetail detail1 = new TransactionDetail(child, BigDecimal.ONE, null, null);
        TransactionDetail detail2 = new TransactionDetail(parent, BigDecimal.ONE, null, null);
        Transaction transaction = new TransactionBuilder().details(detail1, detail2).get();

        List<TransactionCategory> categories = new TransactionUpdate(transaction, singleton(detail1)).getNewCategories();

        assertThat(categories).isEmpty();
    }

    @Test
    public void getNewCategoriesReturnsParentFirst() throws Exception {
        TransactionCategory parent = new TransactionCategory("parent");
        TransactionCategory child = new TransactionCategory(parent, "child");
        TransactionDetail detail = new TransactionDetail(child, BigDecimal.ONE, null, null);
        Transaction transaction = new TransactionBuilder().details(detail).get();

        List<TransactionCategory> categories = new TransactionUpdate(transaction).getNewCategories();

        assertThat(categories).hasSize(2);
        assertThat(categories).containsExactly(parent, child);
    }

    @Test
    public void getNewCategoriesConsolidatesNewCategories() throws Exception {
        TransactionCategory parent = new TransactionCategory(-1L);
        TransactionCategory c1 = new TransactionCategory(parent, "child");
        TransactionCategory c2 = new TransactionCategory(parent, "child");
        TransactionDetail detail1 = new TransactionDetail(c1, BigDecimal.ONE, null, null);
        TransactionDetail detail2 = new TransactionDetail(c2, BigDecimal.ONE, null, null);
        Transaction transaction = new TransactionBuilder().details(detail1, detail2).get();

        List<TransactionCategory> categories = new TransactionUpdate(transaction).getNewCategories();

        assertThat(categories).hasSize(1);
        assertThat(categories).containsExactly(c1);
        assertThat(c1.getParent()).isSameAs(parent);
    }

    @Test
    public void getNewCategoriesConsolidatesNewParents() throws Exception {
        TransactionCategory p1 = new TransactionCategory("parent");
        TransactionCategory p2 = new TransactionCategory("parent");
        TransactionCategory c1 = new TransactionCategory(p2, "child");
        TransactionDetail detail1 = new TransactionDetail(p1, BigDecimal.ONE, null, null);
        TransactionDetail detail2 = new TransactionDetail(c1, BigDecimal.ONE, null, null);
        Transaction transaction = new TransactionBuilder().details(detail1, detail2).get();

        List<TransactionCategory> categories = new TransactionUpdate(transaction).getNewCategories();

        assertThat(categories).hasSize(2);
        assertThat(categories).containsExactly(p1, c1);
        assertThat(c1.getParent()).isSameAs(p1);
    }
}