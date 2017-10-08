package io.github.jonestimd.finance.file;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFieldBuilder;
import org.junit.Test;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

public class ImportFieldMapperTest {
    private final ImportField payeeField = new ImportFieldBuilder().label("Payee").get();
    private final Set<ImportField> fields = Collections.singleton(payeeField);

    private final ImportFieldMapper mapper = new ImportFieldMapper(fields);

    @Test
    public void extractsMappedField() throws Exception {
        Iterable<ListMultimap<ImportField, String>> records = mapper.mapFields(Stream.of(ImmutableMap.of("Payee", "some payee")));

        assertThat(records).hasSize(1);
        Map<ImportField, Collection<String>> record = records.iterator().next().asMap();
        assertThat(record.keySet()).containsExactly(payeeField);
        assertThat(record.get(payeeField)).containsExactly("some payee");
    }

    @Test
    public void ignoresUnmappedFields() throws Exception {
        Iterable<ListMultimap<ImportField, String>> records = mapper.mapFields(Stream.of(ImmutableMap.of("Dummy", "xxx")));

        assertThat(records).hasSize(1);
        assertThat(records.iterator().next().asMap()).isEqualTo(singletonMap(payeeField, singletonList("")));
    }
}