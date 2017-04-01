// The MIT License (MIT)
//
// Copyright (c) 2017 Tim Jones
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
package io.github.jonestimd.finance.plugin;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DerbyDriverConnectionService extends EmbeddedDriverConnectionService {
    private final List<String> defaultPath = Arrays.asList(System.getProperty("user.home"), "Documents", "finances");

    public DerbyDriverConnectionService() {
        super("Derby", "org.hibernate.dialect.DerbyTenSevenDialect", "org.apache.derby.jdbc.EmbeddedDriver", "derby:directory:");
    }

    @Override
    public boolean isEnabled(Field field) {
        return field == Field.DIRECTORY;
    }

    @Override
    public boolean isRequired(Field field) {
        return field == Field.DIRECTORY;
    }

    @Override
    public Map<Field, String> getDefaultValues() {
        Map<Field, String> properties = new HashMap<>();
        properties.put(Field.DIRECTORY, String.join(File.separator, defaultPath));
        return properties;
    }
}
