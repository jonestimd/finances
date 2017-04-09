package io.github.jonestimd.finance.domain.account;

import io.github.jonestimd.finance.swing.BundleType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class AccountTypeTest {
    @Test
    public void toStringReturnsLocalizedValues() {
        for (AccountType type : AccountType.values()) {
            assertThat(type.toString()).isEqualTo(BundleType.REFERENCE.getString("accountType." + type.name()));
        }
    }
}