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
package io.github.jonestimd.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import io.github.jonestimd.finance.config.ConfigManager;

/**
 * @deprecated use {@link ConfigManager}
 */
public class PropertiesLoader {
    private final File propertyFile;

    /**
     * @param fileProperty System property specifying the file to load
     * @param defaultFile the file to use is the System property isn't set
     */
    public PropertiesLoader(String fileProperty, File defaultFile) {
        this.propertyFile = Optional.ofNullable(System.getProperty(fileProperty)).map(File::new).orElse(defaultFile);
    }

    public boolean fileExists() {
        return propertyFile.exists();
    }

    public Properties load() throws IOException {
        try (FileInputStream inputStream = new FileInputStream(propertyFile)) {
            Properties properties = new Properties();
            properties.load(inputStream);
            return properties;
        }
    }
}
