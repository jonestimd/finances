package io.github.jonestimd.finance.domain.fileimport;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class FieldTypeTest {
    @Test
    public void isTransaction() throws Exception {
        assertThat(FieldType.DATE.isTransaction()).isTrue();
        assertThat(FieldType.PAYEE.isTransaction()).isTrue();
        assertThat(FieldType.SECURITY.isTransaction()).isTrue();
        assertThat(FieldType.CATEGORY.isTransaction()).isFalse();
        assertThat(FieldType.TRANSFER_ACCOUNT.isTransaction()).isFalse();
        assertThat(FieldType.AMOUNT.isTransaction()).isFalse();
        assertThat(FieldType.ASSET_QUANTITY.isTransaction()).isFalse();
    }
}