package io.github.jonestimd.finance.file.csv;

import java.io.ByteArrayInputStream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFieldBuilder;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class CsvFieldValueExtractorTest {
    private final ImportField payeeField = new ImportFieldBuilder().label("Payee").get();
    private final ImmutableMap<String, ImportField> fields = ImmutableMap.of("Payee", payeeField);

    private final CsvFieldValueExtractor extractor = new CsvFieldValueExtractor(fields);

    @Test
    public void extractsMappedField() throws Exception {
        Iterable<Multimap<ImportField, String>> records = extractor.parse(inputStream("\"Payee\",\r\n,\"some payee\""));

        assertThat(records).hasSize(1);
    }

    @Test
    public void ignoresUnmappedFields() throws Exception {
        Iterable<Multimap<ImportField, String>> records = extractor.parse(inputStream("\"Dummy\"\r\n,\"xxx\""));

        assertThat(records).hasSize(1);
        assertThat(records.iterator().next().asMap()).isEmpty();
    }

    protected ByteArrayInputStream inputStream(String data) {
        return new ByteArrayInputStream(data.getBytes());
    }
}