package io.github.jonestimd.finance.file.quicken.qif;

import io.github.jonestimd.finance.domain.asset.AssetType;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.operations.AssetOperations;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecurityConverterTest {
    private AssetOperations securityOperations = mock(AssetOperations.class);
    private SecurityConverter converter = new SecurityConverter(securityOperations);

    @Test
    public void getTypesIncludesSecurity() throws Exception {
        assertThat(converter.getTypes().contains("Type:Security")).isTrue();
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
        assertThat(security.getType()).isEqualTo(record.getValue(TYPE));
        assertThat(security.getName()).isEqualTo(record.getValue(NAME));
        assertThat(security.getSymbol()).isEqualTo(record.getValue(SECURITY_SYMBOL));
        assertThat(security.getScale()).isEqualTo(AssetType.SECURITY.getDefaultScale());
    }
}