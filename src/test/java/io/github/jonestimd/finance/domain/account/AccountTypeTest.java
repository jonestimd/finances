package io.github.jonestimd.finance.domain.account;

import io.github.jonestimd.finance.swing.BundleType;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccountTypeTest {
    @Test
    public void toStringReturnsLocalizedValues() {
        for (AccountType type : AccountType.values()) {
            assertEquals(BundleType.REFERENCE.getString("accountType." + type.name()), type.toString());
        }
    }
}