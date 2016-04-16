package io.github.jonestimd.finance.file.quicken.qif;

import io.github.jonestimd.finance.domain.asset.AssetType;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.operations.AssetOperations;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SecurityConverterTest {
    private AssetOperations securityOperations = mock(AssetOperations.class);
    private SecurityConverter converter = new SecurityConverter(securityOperations);

    @Test
    public void getTypesIncludesSecurity() throws Exception {
        assertTrue(converter.getTypes().contains("Type:Security"));
    }

    @Test
    public void importRecordCopiesFields() throws Exception {
        QifRecord record = new QifRecord(1L);
        record.setValue(NAME, "name");
        record.setValue(SECURITY_SYMBOL, "symbol");
        record.setValue(TYPE, "type");

        converter.importRecord(null, record);

        ArgumentCaptor<Security> capture = ArgumentCaptor.forClass(Security.class);
        verify(securityOperations).createIfUnique(capture.capture());
        Security security = capture.getValue();
        assertEquals(record.getValue(TYPE), security.getType());
        assertEquals(record.getValue(NAME), security.getName());
        assertEquals(record.getValue(SECURITY_SYMBOL), security.getSymbol());
        assertEquals(AssetType.SECURITY.getDefaultScale(), security.getScale());
    }
}