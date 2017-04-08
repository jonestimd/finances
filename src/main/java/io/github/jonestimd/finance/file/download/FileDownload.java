// The MIT License (MIT)
//
// Copyright (c) 2016 Tim Jones
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package io.github.jonestimd.finance.file.download;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.jonestimd.finance.dao.DaoRepository;
import io.github.jonestimd.finance.dao.HibernateDaoContext;
import io.github.jonestimd.finance.operations.FileImportOperationsImpl;
import io.github.jonestimd.finance.service.ServiceContext;
import io.github.jonestimd.util.PropertiesLoader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import static io.github.jonestimd.finance.swing.FinanceApplication.*;

public class FileDownload {
    private static final Logger LOGGER = Logger.getLogger(FileDownload.class);

    private final Config config;
    private final CloseableHttpClient client;
    private final DownloadContext context;
    private final RequestFactory requestFactory;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("usage: java " + FileDownload.class.getName() + " config_name [import_name]");
            System.exit(1);
        }
        try {
            Config config = ConfigFactory.parseFile(getConfigFile()).resolve().getConfig(args[0]);
            DownloadContext context = new DownloadContext(config);
            FileDownload download = new FileDownload(config, context, buildClient(config));
            download.downloadNewStatements();
            if (args.length > 1) {
                DaoRepository daoContext = new HibernateDaoContext(new PropertiesLoader(CONNECTION_FILE_PROPERTY, DEFAULT_CONNECTION_PROPERTIES).load());
                ServiceContext serviceContext = new ServiceContext(daoContext);
                FileImportOperationsImpl fileImportOperations = new FileImportOperationsImpl(daoContext.getImportFileDao(), serviceContext);
                for (File file : context.getStatements()) {
                    fileImportOperations.importTransactions(args[1], new FileInputStream(file));
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Error processing download: " + args[0], ex);
        }
    }

    protected static CloseableHttpClient buildClient(Config config) {
        HttpClientBuilder clientBuilder = HttpClients.custom();
        if (config.hasPath("credentials")) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(new AuthScope(null, -1),
                    new UsernamePasswordCredentials(config.getString("credentials.username"), config.getString("credentials.password")));
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
        return clientBuilder.build();
    }

    private static File getConfigFile() {
        return new File(new File(System.getProperty("user.home")), ".finances/download.conf");
    }

    public FileDownload(Config config, DownloadContext context, CloseableHttpClient client) {
        this.config = config;
        this.context = context;
        this.client = client;
        requestFactory = new RequestFactory(context);
    }

    public void downloadNewStatements() throws IOException, ParseException {
        for (Config step : config.getConfigList("fileList")) {
            execute(step);
        }
        for (List<Object> fileKeys : context.getFileList()) {
            if (!context.getFile(fileKeys).exists()) {
                for (Config download : config.getConfigList("download")) {
                    execute(download, fileKeys);
                }
            }
        }
    }

    protected void execute(Config step) throws IOException, ParseException {
        execute(step, Collections.emptyList());
    }

    protected void execute(Config step, List<Object> fileKeys) throws IOException, ParseException {
        for (int tries=0; true; ) {
            try {
                executeOnce(step, fileKeys);
                return;
            } catch (IOException ex) {
                if (tries++ < 3) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                else throw ex;
            }
        }
    }

    private void executeOnce(Config step, List<Object> fileKeys) throws IOException, ParseException {
        HttpUriRequest request = requestFactory.request(step, fileKeys);
        try (CloseableHttpResponse response = client.execute(request)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Download failed: " + response.getStatusLine().toString());
            }
            if (step.hasPath("result")) {
                StepResponse<?> result = getResponse(step.getString("result.format"), response.getEntity());
                if (step.hasPath("result.extract")) {
                    for (Config extract : step.getConfigList("result.extract")) {
                        String name = extract.getString("name");
                        context.putValue(name, result.getValue(extract));
                    }
                }
                if (step.hasPath("result.files")) {
                    result.getValues(step.getConfig("result.files")).forEach(context::addFile);
                }
            }
            else if (step.hasPath("save")) {
                File file = context.getFile(fileKeys);
                new SaveConsumer(file).accept(response.getEntity());
                context.addStatement(file);
            }
            else {
                EntityUtils.consume(response.getEntity());
            }
        }
    }

    private StepResponse<?> getResponse(String type, HttpEntity entity) throws IOException {
        if ("json".equals(type)) {
            return new JsonResponse(entity);
        }
        if ("html".equals(type)) {
            return new HtmlResponse(entity, context.getBaseUrl());
        }
        throw new IllegalArgumentException("Invalid response type: " + type);
    }
}
