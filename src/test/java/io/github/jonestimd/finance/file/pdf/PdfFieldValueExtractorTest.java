package io.github.jonestimd.finance.file.pdf;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.itextpdf.text.pdf.parser.Vector;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFieldBuilder;
import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class PdfFieldValueExtractorTest {
    private final ImportField dateField = new ImportFieldBuilder().label("Date:").rightEdge(10f).get();
    private final ImportField payeeField = new ImportFieldBuilder().label("The Payee:").rightEdge(20f).get();
    private final ImportField amountField = new ImportFieldBuilder().label("Amount").bounds(20f, 10f, 0f, 5f, 10f, 20f).get();
    private final Map<String, ImportField> importFields = ImmutableMap.of(
            dateField.getLabel(), dateField,
            payeeField.getLabel(), payeeField,
            amountField.getLabel(), amountField);
    private final PdfFieldValueExtractor pdfFieldValueExtractor = new PdfFieldValueExtractor(importFields);
    private final Builder<Vector, String> pdfTextBuilder = ImmutableMap.builder();

    @Test
    public void filtersEmptyValues() throws Exception {
        final float y = 2f;
        addPdfText("The Other_field something", 0f, y);
        addPdfText(payeeField, y, "");

        Multimap<ImportField, String> fieldValues = new PdfFieldValueExtractor(importFields).getFieldValues(pdfTextBuilder.build().entrySet().stream());

        assertThat(fieldValues.asMap()).isEmpty();
    }

    @Test
    public void mapsMultiWordFields() throws Exception {
        final String payee = "payee";
        final float y = 2f;
        addPdfText("The Other_field something", 0f, y);
        addPdfText(payeeField, y, "payee");

        Multimap<ImportField, String> fieldValues = new PdfFieldValueExtractor(importFields).getFieldValues(pdfTextBuilder.build().entrySet().stream());

        assertThat(fieldValues.get(payeeField)).containsOnly(payee);
        assertThat(fieldValues.asMap()).hasSize(1).as("no empty values");
    }

    @Test
    public void mapsFieldsOnSeparateLines() throws Exception {
        final String date = "05/13/1926";
        String payee = "payee";
        addPdfText(dateField, 1f, date);
        addPdfText(payeeField, 2f, "payee");

        Multimap<ImportField, String> fieldValues = pdfFieldValueExtractor.getFieldValues(pdfTextBuilder.build().entrySet().stream());

        assertThat(fieldValues.get(dateField)).containsOnly(date);
        assertThat(fieldValues.get(payeeField)).containsOnly(payee);
    }

    @Test
    public void mapsFieldsOnSameLine() throws Exception {
        final String date = "05/13/1926";
        String payee = "payee";
        addPdfText(dateField, 1f, date);
        addPdfText(payeeField, 1f, "payee");

        Multimap<ImportField, String> fieldValues = pdfFieldValueExtractor.getFieldValues(pdfTextBuilder.build().entrySet().stream());

        assertThat(fieldValues.get(dateField)).containsOnly(date);
        assertThat(fieldValues.get(payeeField)).containsOnly(payee);
    }

    @Test
    public void returnsAllValues() throws Exception {
        final String date1 = "05/13/1926";
        final String date2 = "06/17/1926";
        addPdfText(dateField, 1f, date1);
        addPdfText(dateField, 2f, date2);

        ListMultimap<ImportField, String> fieldValues = pdfFieldValueExtractor.getFieldValues(pdfTextBuilder.build().entrySet().stream());

        assertThat(fieldValues.get(dateField)).containsExactly(date1, date2);
    }

    @Test
    public void combinesMultiwordValues() throws Exception {
        String payee = "some payee";
        addPdfText(payeeField, 1f, "some payee");

        Multimap<ImportField, String> fieldValues = pdfFieldValueExtractor.getFieldValues(pdfTextBuilder.build().entrySet().stream());

        assertThat(fieldValues.get(payeeField)).containsOnly(payee);
    }

    @Test
    public void doesNotCombineValuesFromDifferentLines() throws Exception {
        String payee = "some payee";
        addPdfText(payeeField, 1f, "some payee");
        addPdfText("ignored", payeeField.getRegion().getValueRight(), 1.1f);

        Multimap<ImportField, String> fieldValues = pdfFieldValueExtractor.getFieldValues(pdfTextBuilder.build().entrySet().stream());

        assertThat(fieldValues.get(payeeField)).containsOnly(payee);
    }

    @Test
    public void matchesFieldByBoundingBox() throws Exception {
        addPdfText("Amount", 0f, 0f);
        addPdfText("above", 10f, 0f);
        addPdfText("Amount", 0f, 20f);
        addPdfText("left", 9f, 20f);
        addPdfText("the amount", 10f, 20f);
        addPdfText("right", 21f, 20f);
        addPdfText("Amount", 0f, 30f);
        addPdfText("below", 10f, 30f);

        Multimap<ImportField, String> fieldValues = pdfFieldValueExtractor.getFieldValues(pdfTextBuilder.build().entrySet().stream());

        assertThat(fieldValues.get(amountField)).containsOnly("the amount");
    }

    protected void addPdfText(ImportField field, float y, String text) {
        String label = field.getLabel().toUpperCase();
        float x = field.getRegion().getValueRight() - label.split(" ").length - text.split(" ").length;
        x = addPdfText(label, x, y);
        addPdfText(text, x, y);
    }

    private float addPdfText(String text, float x, float y) {
        for (String word : text.split(" ")) {
            pdfTextBuilder.put(new Vector(x, y, 0f), word);
            x += 1f;
        }
        return x;
    }
}