package io.github.jonestimd.util;

import java.io.File;
import java.util.Properties;

import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class PropertiesLoaderTest {
    private static final String TEST_FILE = "PropertiesLoaderTest.properties";
    private static final String DEFAULT_FILE = "default.properties";
    private static final String FILE_PROPERTY = "test.filename";

    @Test
    public void readsFileSpecifiedByProperty() throws Exception {
        String defaultFile = getClass().getResource(DEFAULT_FILE).getPath();
        String filename = getClass().getResource(TEST_FILE).getPath();
        System.setProperty(FILE_PROPERTY, filename);

        Properties properties = new PropertiesLoader(FILE_PROPERTY, new File(defaultFile)).load();

        assertThat(properties.getProperty("did.it.load")).isEqualTo("yes it did");
    }

    @Test
    public void readsDefaultFileIfPropertyNotSet() throws Exception {
        String defaultFile = getClass().getResource(DEFAULT_FILE).getPath();
        System.getProperties().remove(FILE_PROPERTY);

        Properties properties = new PropertiesLoader(FILE_PROPERTY, new File(defaultFile)).load();

        assertThat(properties.getProperty("did.it.load")).isEqualTo("no it didn't");
    }

    @Test
    public void fileExistsReturnsTrueForExistingFile() throws Exception {
        String defaultFile = getClass().getResource(DEFAULT_FILE).getPath();
        PropertiesLoader loader = new PropertiesLoader(FILE_PROPERTY, new File(defaultFile));

        assertThat(loader.fileExists()).isTrue();
    }

    @Test
    public void fileExistsReturnsFalseForNonexistentFile() throws Exception {
        PropertiesLoader loader = new PropertiesLoader(FILE_PROPERTY, new File("unknown"));

        assertThat(loader.fileExists()).isFalse();
    }
}