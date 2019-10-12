package io.github.jonestimd.finance.file.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.github.jonestimd.function.FailableConsumer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;

public class PdfStreamBuilder {
    private final PDDocument document = new PDDocument();
    private PDPage page;
    private PDPageContentStream stream;
    private boolean textMode;

    public PdfStreamBuilder startPage() throws IOException {
        page = new PDPage();
        stream = new PDPageContentStream(document, page);
        return this;
    }

    public PdfStreamBuilder setFont() throws IOException {
        return setFont(PDType1Font.HELVETICA, 12f);
    }

    public PdfStreamBuilder setFont(PDFont font, float size) throws IOException {
        stream.setFont(font, size);
        return this;
    }

    public PdfStreamBuilder endPage() throws IOException {
        stream.close();
        document.addPage(page);
        return this;
    }

    public PdfStreamBuilder beginText() throws IOException {
        stream.beginText();
        textMode = true;
        return this;
    }

    public PdfStreamBuilder endText() throws IOException {
        stream.endText();
        textMode = false;
        return this;
    }

    public PdfStreamBuilder newLineAtOffset(float tx, float ty) throws IOException {
        stream.newLineAtOffset(tx, ty);
        return this;
    }

    public PdfStreamBuilder setTextMatrix(float tx, float ty) throws IOException {
        return setTextMatrix(1, 1, tx, ty);
    }

    public PdfStreamBuilder setTextMatrix(float sx, float sy, float tx, float ty) throws IOException {
        stream.setTextMatrix(new Matrix(sx, 0, 0, sy, tx, ty));
        return this;
    }

    public PdfStreamBuilder addText(String text) throws IOException {
        return ensureTextMode(stream::showText, text);
    }

    public PdfStreamBuilder addTextWithPositioning(Object... args) throws IOException {
        return ensureTextMode(stream::showTextWithPositioning, args);
    }

    private <T> PdfStreamBuilder ensureTextMode(FailableConsumer<T, IOException> instruction, T args) throws IOException {
        if (!textMode) stream.beginText();
        instruction.accept(args);
        if (!textMode) stream.endText();
        return this;
    }

    public InputStream getStream() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
