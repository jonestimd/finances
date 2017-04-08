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
package io.github.jonestimd.finance.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;

public class ConfigManager {
    public static final String CONFIG_FILE_PROPERTY = "config";
    public static final String DEFAULT_CONFIG_FILE = "~/.finances/finances.conf";
    public static final String CONNECTION_PATH = "connection.default";

    private final File configFile;
    private Config config;

    public ConfigManager() {
        this(CONFIG_FILE_PROPERTY, DEFAULT_CONFIG_FILE);
    }

    public ConfigManager(String fileProperty, String defaultFile) {
        String fileName = System.getProperty(fileProperty, defaultFile);
        if (fileName.startsWith("~/")) fileName = System.getProperty("user.home") + fileName.substring(1);
        this.configFile = new File(fileName);
        if (configFile.exists()) config = ConfigFactory.parseFile(configFile);
        else config = ConfigFactory.empty();
    }

    public Config root() {
        return config;
    }

    public Optional<Config> get(String path) {
        return config.hasPath(path) ? Optional.of(config.getConfig(path)) : Optional.empty();
    }

    public ConfigManager addPath(String path, Config config) {
        this.config = this.config.withValue(path, config.root());
        return this;
    }

    public void save(boolean isPrivate) {
        if (!configFile.getParentFile().exists()) configFile.getParentFile().mkdirs();
        ConfigRenderOptions renderOptions = ConfigRenderOptions.concise().setFormatted(true).setJson(false);
        try (FileWriter stream = new FileWriter(configFile)) {
            stream.write(config.root().render(renderOptions));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        if (configFile.exists() && isPrivate) {
            try {
                Files.setPosixFilePermissions(configFile.toPath(), PosixFilePermissions.fromString("rw-------"));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
