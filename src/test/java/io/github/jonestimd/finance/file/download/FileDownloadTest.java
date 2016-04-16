package io.github.jonestimd.finance.file.download;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileDownloadTest {
    @Mock
    private Config config;
    @Mock
    private DownloadContext context;
    @Mock
    private CloseableHttpClient client;
    @InjectMocks
    private FileDownload download;

    @Test
    public void buildClientWithoutCredentials() throws Exception {
        final Config config = ConfigFactory.parseString("{ url = \"http://example.com\", output.path = /home/user/downloads, saveAs = \"%1$s.pdf\" }");

        CloseableHttpClient client = FileDownload.buildClient(config);

        Field field = client.getClass().getDeclaredField("credentialsProvider");
        field.setAccessible(true);
        Object provider = field.get(client);
        assertThat(provider).isInstanceOf(BasicCredentialsProvider.class);
        BasicCredentialsProvider basicProvider = (BasicCredentialsProvider) provider;
        assertThat(basicProvider.getCredentials(new AuthScope(null, -1))).isNull();
    }

    @Test
    public void buildClientWithCredentials() throws Exception {
        final Config config = ConfigFactory.parseString("{ url = \"http://example.com\", output.path = /home/user/downloads, saveAs = \"%1$s.pdf\", " +
                "credentials { username = theUser, password = thePassword } }");

        CloseableHttpClient client = FileDownload.buildClient(config);

        Field field = client.getClass().getDeclaredField("credentialsProvider");
        field.setAccessible(true);
        Object provider = field.get(client);
        assertThat(provider).isInstanceOf(BasicCredentialsProvider.class);
        BasicCredentialsProvider basicProvider = (BasicCredentialsProvider) provider;
        Credentials credentials = basicProvider.getCredentials(new AuthScope(null, -1));
        assertThat(credentials.getUserPrincipal().getName()).isEqualTo("theUser");
        assertThat(credentials.getPassword()).isEqualTo("thePassword");
    }

    @Test(expected = RuntimeException.class)
    public void downloadNewStatementsThrowsExceptionForStatusNotOk() throws Exception {
        final Config filesStep = ConfigFactory.parseString("{ method = get, path = /files }");
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        doReturn(Collections.singletonList(filesStep)).when(config).getConfigList("fileList");
        when(client.execute(any(HttpGet.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_NOT_FOUND, null));

        download.downloadNewStatements();
    }

    @Test
    public void downloadNewStatementsClosesEntityStream() throws Exception {
        final Config filesStep = ConfigFactory.parseString("{ method = get, path = /files }");
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final InputStream stream = mock(InputStream.class);
        doReturn(Collections.singletonList(filesStep)).when(config).getConfigList("fileList");
        when(client.execute(any(HttpGet.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null));
        when(response.getEntity()).thenReturn(entity);
        when(entity.isStreaming()).thenReturn(true);
        when(entity.getContent()).thenReturn(stream);

        download.downloadNewStatements();

        verify(response).getEntity();
        verify(entity).getContent();
        verify(stream).close();
    }

    @Test
    public void downloadNewStatementsCapturesResult() throws Exception {
        final Config filesStep = ConfigFactory.parseString("{ method = get, path = /files, " +
                "result = { format = json, extract = [{ name = var, selector = x }] } }");
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final InputStream stream = new ByteArrayInputStream("{\"x\":\"value\"}".getBytes("UTF-8"));
        doReturn(Collections.singletonList(filesStep)).when(config).getConfigList("fileList");
        when(client.execute(any(HttpGet.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null));
        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(stream);

        download.downloadNewStatements();

        verify(response).getEntity();
        verify(context).putValue("var", "value");
    }

    @Test
    public void downloadNewStatementsCapturesFileList() throws Exception {
        final Config filesStep = ConfigFactory.parseString("{ method = get, path = /files, " +
                "result = { format = html, files.selector = { path = li, fields = [\"text()\"] } } }");
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final InputStream stream = new ByteArrayInputStream("<html><body><li>file url</li></body></html>".getBytes("UTF-8"));
        doReturn(Collections.singletonList(filesStep)).when(config).getConfigList("fileList");
        when(context.getBaseUrl()).thenReturn("http://example.com");
        when(client.execute(any(HttpGet.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null));
        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(stream);

        download.downloadNewStatements();

        verify(response).getEntity();
        verify(context).addFile(Collections.singletonList("file url"));
    }

    @Test(expected = RuntimeException.class)
    public void downloadNewStatementsThrowsExceptionForInvalidResultFormat() throws Exception {
        final Config filesStep = ConfigFactory.parseString("{ method = get, path = /files, " +
                "result = { format = HTML, files.selector = { path = li, fields = [\"text()\"] } } }");
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        doReturn(Collections.singletonList(filesStep)).when(config).getConfigList("fileList");
        when(client.execute(any(HttpGet.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null));

        download.downloadNewStatements();
    }

    @Test
    public void downloadNewStatementsSavesNewFile() throws Exception {
        final Config downloadStep = ConfigFactory.parseString("{ method = get, path = /files, save = true }");
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final HttpEntity entity = mock(HttpEntity.class);
        final InputStream stream = new ByteArrayInputStream("statement content".getBytes("UTF-8"));
        final List<Object> fileKeys = Collections.singletonList("file key");
        final File statement = new File("./test.txt");
        if (statement.exists()) statement.delete();
        doReturn(Collections.emptyList()).when(config).getConfigList("fileList");
        doReturn(Collections.singletonList(downloadStep)).when(config).getConfigList("download");
        when(context.getFileList()).thenReturn(Collections.singletonList(fileKeys));
        when(context.getFile(fileKeys)).thenReturn(statement);
        when(client.execute(any(HttpGet.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null));
        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(stream);

        download.downloadNewStatements();

        verify(response).getEntity();
        verify(context).addStatement(statement);
    }

    @Test
    public void downloadNewStatementsSkipsExistingFile() throws Exception {
        final Config downloadStep = ConfigFactory.parseString("{ method = get, path = /files, save = true }");
        final CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        final List<Object> fileKeys = Collections.singletonList("file key");
        final File statement = new File("./test.txt");
        statement.createNewFile();
        doReturn(Collections.emptyList()).when(config).getConfigList("fileList");
        doReturn(Collections.singletonList(downloadStep)).when(config).getConfigList("download");
        when(context.getFileList()).thenReturn(Collections.singletonList(fileKeys));
        when(context.getFile(fileKeys)).thenReturn(statement);

        download.downloadNewStatements();

        verifyZeroInteractions(client);
        statement.deleteOnExit();
    }
}