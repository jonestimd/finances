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

import java.awt.Window;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.*;
import java.util.*;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.github.jonestimd.finance.plugin.DriverConfigurationService;
import io.github.jonestimd.finance.plugin.DriverConfigurationService.DriverService;
import io.github.jonestimd.finance.swing.database.ConnectionDialog;

public class ConnectionConfig {
    public static final String CONNECTION_FILE_PROPERTY = "connection";
    public static final String DEFAULT_CONFIG_FILE = "~/.finances/connection.conf";
    public static final String CONNECTION_PATH = "connection.default";

    private final File configFile;
    private Config config;

    public ConnectionConfig() {
        this(CONNECTION_FILE_PROPERTY, DEFAULT_CONFIG_FILE);
    }

    protected ConnectionConfig(String fileProperty, String defaultFile) {
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

    public ConnectionConfig addPath(String path, Config config) {
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
            Path configPath = configFile.toPath();
            try {
                Files.setPosixFilePermissions(configPath, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ex) {
                setWindowsPermissions(configPath);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void setWindowsPermissions(Path path) {
        try {
            AclFileAttributeView aclAttr = Files.getFileAttributeView(path, AclFileAttributeView.class);
            UserPrincipalLookupService lookupService = path.getFileSystem().getUserPrincipalLookupService();
            aclAttr.setAcl(Arrays.asList(
                    getAclEntry(lookupService.lookupPrincipalByName("SYSTEM"), Collections.emptySet()),
                    getAclEntry(lookupService.lookupPrincipalByName(System.getProperty("user.name")), EnumSet.allOf(AclEntryPermission.class))
            ));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private AclEntry getAclEntry(UserPrincipal user, Set<AclEntryPermission> permissions) throws IOException {
        return AclEntry.newBuilder().setPermissions(permissions)
                .setPrincipal(user)
                .setType(AclEntryType.ALLOW)
                .build();
    }

    public DriverService loadDriver() {
        return loadDriver(null).get();
    }

    public Optional<DriverService> loadDriver(Window initialFrame) {
        Optional<Config> configOption = get(ConnectionConfig.CONNECTION_PATH);
        if (! configOption.isPresent() && initialFrame != null) {
            configOption = new ConnectionDialog(initialFrame).showDialog();
            configOption.ifPresent(config -> addPath(ConnectionConfig.CONNECTION_PATH, config).save(true));
        }
        return configOption.map(DriverConfigurationService::forConfig);
    }

    public static Properties asProperties(Config config, String path) {
        final String prefix = path + ".";
        final Properties properties = new Properties();
        if (config.hasPath(path)) {
            config.getConfig(path).entrySet().forEach(entry -> {
                properties.setProperty(prefix + entry.getKey(), entry.getValue().unwrapped().toString());
            });
        }
        return properties;
    }
}
