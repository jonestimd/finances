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
package io.github.jonestimd.finance.swing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class PropertiesPersister {
    private static final Logger logger = Logger.getLogger(PropertiesPersister.class);

    private File settingsFile;

    public PropertiesPersister(String settingsFilename) {
        settingsFile = new File(System.getProperty("user.home"), settingsFilename);
        loadSettings();
    }

    private void loadSettings() {
        Properties settings = new Properties(System.getProperties());
        System.setProperties(settings);
        if (settingsFile.exists()) {
            try {
                settings.load(new FileInputStream(settingsFile));
            } catch (Exception ex) {
                logger.warn("failed to read settings", ex);
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new SettingsShutdownHook()));
    }

    private class SettingsShutdownHook implements Runnable {
        public void run() {
            try {
                if (! settingsFile.getParentFile().exists()) {
                    settingsFile.getParentFile().mkdirs();
                }
                System.getProperties().store(new FileOutputStream(settingsFile), "finances user settings");
            } catch (Exception ex) {
                logger.warn("failed to write settings", ex);
            }
        }
    }
}
