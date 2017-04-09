package io.github.jonestimd.finance.file.quicken.qif;

import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ClassConverterTest {

    @Test
    public void getTypesIncludesClasses() throws Exception {
        ClassConverter converter = new ClassConverter(null);

        assertThat(converter.getTypes().contains("Type:Class")).isTrue();
    }

    @Test
    public void importRecordCopiesFields() throws Exception {
        TransactionGroupOperations txGroupOperations = mock(TransactionGroupOperations.class);
        QifRecord record = new QifRecord(1L);
        record.setValue(NAME, "name");
        record.setValue(DESCRIPTION, "description");
        ClassConverter converter = new ClassConverter(txGroupOperations);

        converter.importRecord(null, record);

        ArgumentCaptor<TransactionGroup> capture = ArgumentCaptor.forClass(TransactionGroup.class);
        verify(txGroupOperations).getOrCreateTransactionGroup(capture.capture());
        TransactionGroup group = capture.getValue();
        assertThat(group.getName()).isEqualTo(record.getValue(NAME));
        assertThat(group.getDescription()).isEqualTo(record.getValue(DESCRIPTION));
    }
}
