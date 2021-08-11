package io.github.jonestimd.finance.file.pdf;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.util.Vector;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class TextExtractorTest {
    @Test
    public void getText_sortsTextByPosition() throws Exception {
        InputStream stream = new PdfStreamBuilder()
                .startPage().setFont()
                .beginText().setTextMatrix(200f, 100f).addText("bottom right").endText()
                .beginText().setTextMatrix(100f, 100f).addText("bottom left").endText()
                .beginText().setTextMatrix(100f, 200f).addText("top left").endText()
                .beginText().setTextMatrix(150f, 200f).addText("top right").endText()
                .endPage()
                .getStream();

        List<Pair<Vector, String>> text = new TextExtractor(stream).getText().collect(Collectors.toList());

        assertThat(text.stream().map(Pair::getValue))
                .containsExactly("top left", "top right", "bottom left", "bottom right");
    }
}