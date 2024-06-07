package io.github.jonestimd.finance.stockquote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class HttpServerTest {
    protected static HttpServer server;
    protected final String basePath = getClass().getPackage().getName().replaceAll("\\.", "/");

    @BeforeClass
    public static void startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 9999), 5);
            server.createContext("/", HttpServerTest::handleRequest);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().toString().substring(1);
        OutputStream outputStream = exchange.getResponseBody();
        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            exchange.getRequestBody().close();
            if (stream != null) {
                exchange.sendResponseHeaders(200, 0);
                int ch;
                while ((ch = stream.read()) >= 0) outputStream.write(ch);
            }
            else exchange.sendResponseHeaders(404, 0);
        } catch (Exception ex) {
            exchange.sendResponseHeaders(500, 0);
        } finally {
            outputStream.close();
        }
    }

    @AfterClass
    public static void stopServer() {
        server.stop(0);
    }

    protected String getUrl(String fileName) {
        return String.format("http://localhost:%d/%s/%s", server.getAddress().getPort(), basePath, fileName);
    }
}
