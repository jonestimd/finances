package io.github.jonestimd.finance.file.quicken.qif;

import io.github.jonestimd.finance.domain.transaction.TransactionGroup;
import io.github.jonestimd.finance.operations.TransactionGroupOperations;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

public class ClassConverterTest {

    @Test
    public void getTypesIncludesClasses() throws Exception {
        ClassConverter converter = new ClassConverter(null);

        assertTrue(converter.getTypes().contains("Type:Class"));
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
        assertEquals(record.getValue(NAME), group.getName());
        assertEquals(record.getValue(DESCRIPTION), group.getDescription());
    }
}
