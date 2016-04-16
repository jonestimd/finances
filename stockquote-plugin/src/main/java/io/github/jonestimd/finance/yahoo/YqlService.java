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
package io.github.jonestimd.finance.yahoo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public class YqlService {
    private static final Logger LOGGER = Logger.getLogger(YqlService.class);
    private static final String ESCAPE_CHARS = " ,\"";
    private static final String YQL_BASE_URL = "query.url.base";
    private static final String YQL_URL_SUFFIX = "query.url.suffix";
    private static final Properties CONFIG = new Properties();
    static {
        try {
            CONFIG.load(YqlService.class.getResourceAsStream("yql.properties"));
        } catch (IOException ex) {
            LOGGER.error("failed to read configuration", ex);
        }
    }

    public <T> List<T> execute(String query, JsonMapper<T> jsonMapper) throws IOException {
        InputStream inputStream = new URL(CONFIG.getProperty(YQL_BASE_URL) + escape(query) + CONFIG.getProperty(YQL_URL_SUFFIX)).openStream();
        return jsonMapper.fromResult(inputStream);
    }

    private String escape(String s) {
        for (int i = 0; i < ESCAPE_CHARS.length(); i++) {
            s = s.replace(ESCAPE_CHARS.substring(i, i+1), String.format("%%%02X", (int) ESCAPE_CHARS.charAt(i)));
        }
        return s;
    }
}
