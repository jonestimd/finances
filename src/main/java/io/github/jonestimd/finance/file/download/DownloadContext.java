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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.MoreObjects;
import com.typesafe.config.Config;

public class DownloadContext {
    private static final Pattern RENDER_PATTERN = Pattern.compile("(\\\\*)?%([dvf])\\{([^}]+)\\}");
    private final Map<String, Object> values = new HashMap<>();
    private final List<List<Object>> fileList = new ArrayList<>();
    private final List<File> statements = new ArrayList<>();
    private final String baseUrl;
    private final File outputPath;
    private final String fileNameFormat;

    public DownloadContext(Config config) {
        baseUrl = config.getString("url");
        outputPath = new File(config.getString("output.path"));
        fileNameFormat = config.getString("saveAs");
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public File getFile(List<Object> fileKeys) {
        return new File(outputPath, String.format(fileNameFormat, fileKeys.toArray()));
    }

    public void putValue(String name, Object value) {
        values.put(name, value);
    }

    public String getString(String name) {
        return (String) values.get(name);
    }

    public void addFile(List<Object> fileKeys) {
        fileList.add(fileKeys);
    }

    public List<List<Object>> getFileList() {
        return fileList;
    }

    public void addStatement(File file) {
        statements.add(file);
    }

    public List<File> getStatements() {
        return statements;
    }

    public String render(String format, List<Object> fileKeys) {
        Matcher matcher = RENDER_PATTERN.matcher(format);
        StringBuffer buffer = new StringBuffer(format.length());
        int start = 0;
        while (matcher.find()) {
            String prefix = MoreObjects.firstNonNull(matcher.group(1), "");
            if (prefix.length() % 2 == 1) {
                matcher.appendReplacement(buffer, prefix.substring(1) + '%');
                start += matcher.start(2);
                matcher.reset(format.substring(start));
            }
            else {
                if (matcher.group(2).equals("d")) {
                    matcher.appendReplacement(buffer, prefix + new SimpleDateFormat(matcher.group(3)).format(new Date()));
                }
                else if (matcher.group(2).equals("v")) {
                    matcher.appendReplacement(buffer, prefix + values.get(matcher.group(3)));
                }
                else {
                    matcher.appendReplacement(buffer, prefix + fileKeys.get(Integer.parseInt(matcher.group(3))));
                }
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
