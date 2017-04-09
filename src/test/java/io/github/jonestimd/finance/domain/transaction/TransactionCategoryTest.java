package io.github.jonestimd.finance.domain.transaction;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class TransactionCategoryTest {
    @Test
    public void compareToGroupsByParent() throws Exception {
        TransactionCategory tax = new TransactionCategory("Tax");
        TransactionCategory taxFed = new TransactionCategory(tax, "Fed");
        TransactionCategory taxState = new TransactionCategory(tax, "State");
        TransactionCategory taxIl = new TransactionCategory(taxState, "IL");
        TransactionCategory taxMo = new TransactionCategory(taxState, "MO");
        TransactionCategory taxSpouse = new TransactionCategory("Tax:Spouse");
        TransactionCategory taxSpouseFed = new TransactionCategory(taxSpouse, "Fed");
        List<TransactionCategory> categories = Lists.newArrayList(taxSpouseFed, tax, taxFed, taxState, taxIl, taxMo, taxSpouse);

        Collections.sort(categories);

        assertThat(categories).containsSequence(tax, taxFed, taxState, taxIl, taxMo, taxSpouse, taxSpouseFed);
    }
}
