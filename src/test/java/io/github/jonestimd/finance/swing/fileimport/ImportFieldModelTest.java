package io.github.jonestimd.finance.swing.fileimport;

import java.util.ArrayList;
import java.util.Arrays;

import io.github.jonestimd.finance.domain.fileimport.AmountFormat;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.fileimport.ImportType;
import org.junit.Test;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

public class ImportFieldModelTest {
    private ImportFileModel fileModel = ImportFileModel.create(new ImportFile());
    private ImportFieldModel model = ImportFieldModel.create(new ImportField(new ArrayList<>(), null), fileModel);

    @Test
    public void validateType_singleDetailImport() throws Exception {
        fileModel.setImportType(ImportType.SINGLE_DETAIL_ROWS);

        assertThat(model.validateType(null)).isEqualTo("Column type is required");
        assertThat(model.validateType(FieldType.AMOUNT)).isNull();
        assertThat(model.validateType(FieldType.CATEGORY)).isNull();
        assertThat(model.validateType(FieldType.SECURITY)).isNull();
    }

    @Test
    public void validateType_multiDetailImport() throws Exception {
        fileModel.setImportType(ImportType.MULTI_DETAIL_ROWS);

        assertThat(model.validateType(FieldType.AMOUNT)).isEqualTo("Amount column is not valid for multi-detail import");
        assertThat(model.validateType(FieldType.CATEGORY)).isNull();
        assertThat(model.validateType(FieldType.SECURITY)).isNull();
    }

    @Test
    public void validateAmountFormat_multiDetailImport() throws Exception {
        fileModel.setImportType(ImportType.MULTI_DETAIL_ROWS);

        assertThat(model.validateAmountFormat(null)).isEqualTo("Amount format is required");
        assertThat(model.validateAmountFormat(AmountFormat.DECIMAL)).isNull();
    }

    @Test
    public void validateAmountFormat_singleDetailImport_amountColumn() throws Exception {
        fileModel.setImportType(ImportType.SINGLE_DETAIL_ROWS);
        model.setType(FieldType.AMOUNT);

        assertThat(model.validateAmountFormat(null)).isEqualTo("Amount format is required");
        assertThat(model.validateAmountFormat(AmountFormat.DECIMAL)).isNull();
    }

    @Test
    public void validateAmountFormat_singleDetailImport_nonAmountColumn() throws Exception {
        fileModel.setImportType(ImportType.SINGLE_DETAIL_ROWS);
        model.setType(FieldType.CATEGORY);

        assertThat(model.validateAmountFormat(null)).isNull();
        assertThat(model.validateAmountFormat(AmountFormat.DECIMAL)).isEqualTo("Amount format is only valid for amount column");
    }

    @Test
    public void validateLabel() throws Exception {
        assertThat(ImportFieldModel.validateLabel(emptyList())).isEqualTo("Column label is required");
        assertThat(ImportFieldModel.validateLabel(Arrays.asList("one", "two", "one"))).isEqualTo("Column label contains duplicate names");
        assertThat(ImportFieldModel.validateLabel(Arrays.asList("one", ""))).isEqualTo("Column label names must not be blank");
        assertThat(ImportFieldModel.validateLabel(Arrays.asList("one", "twp"))).isNull();
    }

    @Test
    public void isValid() throws Exception {
        testIsValid(true, FieldType.AMOUNT, AmountFormat.DECIMAL, "one");
        testIsValid(false, null, AmountFormat.DECIMAL, "one");
        testIsValid(false, FieldType.AMOUNT, null, "one");
        testIsValid(false, FieldType.AMOUNT, AmountFormat.DECIMAL);
    }

    private void testIsValid(boolean valid, FieldType fieldType, AmountFormat amountFormat, String... labels) {
        model.setLabels(Arrays.asList(labels));
        model.setType(fieldType);
        model.setAmountFormat(amountFormat);

        assertThat(model.isValid()).isEqualTo(valid);
    }
}