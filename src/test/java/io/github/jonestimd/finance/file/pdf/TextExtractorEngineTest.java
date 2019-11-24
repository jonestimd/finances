package io.github.jonestimd.finance.file.pdf;

import java.io.InputStream;

import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class TextExtractorEngineTest {
    @Test
    public void processPage_gathersPageText() throws Exception {
        InputStream stream = new PdfStreamBuilder()
                .startPage()
                .setFont().addText("First group of words").addText("").addTextWithPositioning("A", 5f, "djusted spacing words.")
                .endPage()
                .getStream();
        PDFParser parser = new PDFParser(new RandomAccessBufferedFileInputStream(stream));
        parser.parse();
        PDDocument document = parser.getPDDocument();
        TextExtractorEngine engine = new TextExtractorEngine();

        engine.processPage(document.getPage(0));

        assertThat(engine.getDocumentText().stream().map(PdfTextInfo::getText))
                .containsExactly("First group of words", "Adjusted spacing words.");
    }
}