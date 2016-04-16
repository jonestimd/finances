package io.github.jonestimd.finance.swing.transaction;

import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class TransactionGroupFormatTest {
    @Test
    public void ignoresNonTransactionGroup() throws Exception {
        assertThat(new TransactionGroupFormat().format(null)).isEqualTo("");
    }

    @Test
    public void formatsTransactionGroup() throws Exception {
        TransactionGroup group = new TransactionGroup("name", "description");

        assertThat(new TransactionGroupFormat().format(group)).isEqualTo(group.getName());
    }

    @Test
    public void parsesGroupName() throws Exception {
        Object group = new TransactionGroupFormat().parseObject("name");

        assertThat(((TransactionGroup)group).getName()).isEqualTo("name");
    }
}