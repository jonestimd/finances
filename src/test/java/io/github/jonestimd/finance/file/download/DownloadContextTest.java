package io.github.jonestimd.finance.file.download;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class DownloadContextTest {
    private final Config config = ConfigFactory.parseString("{ url = \"http://example.com\", output.path = /home/user/downloads, saveAs = \"%1$s.pdf\" }");

    @Test
    public void getBaseUrl() throws Exception {
        final DownloadContext context = new DownloadContext(config);

        assertThat(context.getBaseUrl()).isEqualTo("http://example.com");
    }

    @Test
    public void addFile() throws Exception {
        final DownloadContext context = new DownloadContext(config);
        List<Object> fileKeys = Collections.singletonList("file1");

        context.addFile(fileKeys);

        assertThat(context.getFileList()).containsExactly(fileKeys);
    }

    @Test
    public void addStatement() throws Exception {
        final DownloadContext context = new DownloadContext(config);
        File statement = new File("/usr/home/statement.pdf");

        context.addStatement(statement);

        assertThat(context.getStatements()).containsExactly(statement);
    }

    @Test
    public void getFile() throws Exception {
        final DownloadContext context = new DownloadContext(config);

        assertThat(context.getFile(Collections.singletonList("filename"))).isEqualTo(new File("/home/user/downloads", "filename.pdf"));
    }

    @Test
    public void renderDate() throws Exception {
        final DownloadContext context = new DownloadContext(config);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        assertThat(context.render("xxx%d{yyyy-MM-dd}yyy", Collections.emptyList())).isEqualTo("xxx" + date + "yyy");
    }

    @Test
    public void renderEscapedDate() throws Exception {
        final DownloadContext context = new DownloadContext(config);

        assertThat(context.render("xxx\\%d{yyyy-MM-dd}yyy", Collections.emptyList())).isEqualTo("xxx%d{yyyy-MM-dd}yyy");
    }

    @Test
    public void renderEscapeAndDate() throws Exception {
        final DownloadContext context = new DownloadContext(config);
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        assertThat(context.render("xxx\\%d{\\\\%d{yyyy-MM-dd}yyy}", Collections.emptyList())).isEqualTo("xxx%d{\\" + date + "yyy}");
    }

    @Test
    public void renderEscapes() throws Exception {
        final DownloadContext context = new DownloadContext(config);

        assertThat(context.render("\\\\\\%d{$1%%x{\\%d{x}", Collections.emptyList())).isEqualTo("\\%d{$1%%x{%d{x}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void renderVariable() throws Exception {
        final DownloadContext context = new DownloadContext(config);
        context.putValue("var", "value");

        assertThat(context.render("xxx%v{var}yyy", Collections.emptyList())).isEqualTo("xxxvalueyyy");
    }


    @Test
    public void missingVariable() throws Exception {
        final DownloadContext context = new DownloadContext(config);

        assertThat(context.render("xxx %v{var1} yyy", Collections.emptyList())).isEqualTo("xxx null yyy");
    }

    @Test
    public void renderFile() throws Exception {
        final DownloadContext context = new DownloadContext(config);

        assertThat(context.render("%f{0}-%f{1}", Lists.newArrayList("key1", "key2"))).isEqualTo("key1-key2");
    }
}