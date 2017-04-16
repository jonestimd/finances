package io.github.jonestimd.finance.file.quicken.qif;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.function.MessageConsumer;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class QifImportTest {
    private MessageConsumer messageConsumer = mock(MessageConsumer.class);

    @Test
    public void converterCalledForTransactionRecord() throws Exception {
        String type = "type";
        String input = formatInput("!{0}\nI\n^\n", type);
        List<RecordConverter> converters = createConverterMocks(type);
        QifImport qifImport = new QifImport(converters);

        qifImport.importFile(new CharArrayReader(input.toCharArray()), messageConsumer);

        RecordConverter converter = converters.get(0);
        verify(converter).importRecord(isA(AccountHolder.class), isA(QifRecord.class));
    }

    private String formatInput(String format, Object ... args) {
        return MessageFormat.format(format, args);
    }

    private List<RecordConverter> createConverterMocks(String ... types) throws Exception {
        List<RecordConverter> converterMocks = new ArrayList<>(types.length);
        for (String type : types) {
            RecordConverter mock = mock(RecordConverter.class);
            when(mock.getTypes()).thenReturn(Collections.singleton(type));
            converterMocks.add(mock);
        }
        return converterMocks;
    }

    @Test
    public void converterSelectedByControlRecord() throws Exception {
        String type1 = "type1";
        String type2 = "type2";
        String input = formatInput("!{0}\n" + NAME.code() + "\n^\n!{1}\n" + DATE.code() + "\n^\n", type1, type2);
        List<RecordConverter> converters = createConverterMocks(type1, type2);
        QifImport qifImport = new QifImport(converters);

        qifImport.importFile(new CharArrayReader(input.toCharArray()), messageConsumer);

        ArgumentCaptor<QifRecord> capture0 = ArgumentCaptor.forClass(QifRecord.class);
        ArgumentCaptor<QifRecord> capture1 = ArgumentCaptor.forClass(QifRecord.class);
        verify(converters.get(0)).importRecord(isA(AccountHolder.class), capture0.capture());
        verify(converters.get(1)).importRecord(isA(AccountHolder.class), capture1.capture());
        assertThat(capture0.getValue().hasValue(NAME)).isTrue();
        assertThat(capture0.getValue().hasValue(DATE)).isFalse();
        assertThat(capture1.getValue().hasValue(DATE)).isTrue();
        assertThat(capture1.getValue().hasValue(NAME)).isFalse();
    }

    @Test
    public void converterNotChangedByOption() throws Exception {
        String type = "type";
        String input = formatInput("!{0}\n" + NAME.code() + "\n^\n!Option\n" + DATE.code() + "\n^\n!Clear\n"
                + DATE.code() + "\n^\n", type);
        List<RecordConverter> converters = createConverterMocks(type);
        QifImport qifImport = new QifImport(converters);

        qifImport.importFile(new CharArrayReader(input.toCharArray()), messageConsumer);

        RecordConverter converter = converters.get(0);
        verify(converter, times(3)).importRecord(isA(AccountHolder.class), isA(QifRecord.class));
    }

    @Test
    public void exitOnEmptyRecord() throws Exception {
        String type = "type";
        String input = formatInput("!{0}\n" + NAME.code() + "\n^\n^\n" + DATE.code() + "\n^\n" + DATE.code() + "\n^\n", type);
        List<RecordConverter> converters = createConverterMocks(type);
        QifImport qifImport = new QifImport(converters);

        qifImport.importFile(new CharArrayReader(input.toCharArray()), messageConsumer);

        RecordConverter converter = converters.get(0);
        verify(converter).importRecord(isA(AccountHolder.class), isA(QifRecord.class));
    }

    @Test
    public void exceptionThrownForInvalidRecord() throws Exception {
        String type = "type";
        String input = formatInput("!{0}\n" + NAME.code() + "\n!\n", type);
        List<RecordConverter> converters = createConverterMocks(type);
        QifImport qifImport = new QifImport(converters);

        try {
            qifImport.importFile(new CharArrayReader(input.toCharArray()), messageConsumer);
            fail("expected an exception");
        }
        catch (QuickenException ex) {
            assertThat(ex.getMessageKey()).isEqualTo("import.qif.invalidRecord");
            assertThat(ex.getMessageArgs()).isEqualTo(new Object[] {"QIF", 2L});
        }
    }

    @Test
    public void quickenExceptionsNotCaught() throws Exception {
        String type = "type";
        String input = formatInput("!{0}\n" + NAME.code() + "\n^\n^\n" + DATE.code() + "\n^\n" + DATE.code() + "\n^\n", type);
        List<RecordConverter> converters = createConverterMocks(type);
        RecordConverter converter = converters.get(0);
        QuickenException cause = new QuickenException("key", "one", 1L);
        doThrow(cause).when(converter).importRecord(isA(AccountHolder.class), isA(QifRecord.class));
        QifImport qifImport = new QifImport(converters);

        try {
            qifImport.importFile(new CharArrayReader(input.toCharArray()), messageConsumer);
            fail("expected an exception");
        } catch (QuickenException ex) {
            assertThat(ex).isSameAs(cause);
        }
    }

    @Test
    public void importExceptionsIncludeLineNumber() throws Exception {
        Reader reader = mock(Reader.class);
        IOException cause = new IOException();
        when(reader.read(any(char[].class), anyInt(), anyInt())).thenThrow(cause);
        List<RecordConverter> converters = createConverterMocks("type");
        QifImport qifImport = new QifImport(converters);

        try {
            qifImport.importFile(reader, messageConsumer);
            fail("expected an exception");
        } catch (QuickenException ex) {
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getMessageKey()).isEqualTo("import.qif.importFailed");
            assertThat(ex.getMessageArgs()).isEqualTo(new Object[] {"QIF", 1L});
        }
    }
}