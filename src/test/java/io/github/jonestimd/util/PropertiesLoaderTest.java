package io.github.jonestimd.util;

import java.io.File;
import java.util.Properties;

import org.junit.Test;

import static org.fest.assertions.Assertions.*;

public class PropertiesLoaderTest {
    private static final String TEST_FILE = "PropertiesLoaderTest.properties";
    private static final String DEFAULT_FILE = "default.properties";
    private static final String FILE_PROPERTY = "test.filename";

    @Test
    public void readsSpecifiedFile() throws Exception {
        String filename = getClass().getResource(TEST_FILE).getPath();

        Properties properties = PropertiesLoader.load(new File(filename));

        assertThat(properties.getProperty("did.it.load")).isEqualTo("yes it did");
    }

    @Test
    public void readsFileSpecifiedByProperty() throws Exception {
        String defaultFile = getClass().getResource(DEFAULT_FILE).getPath();
        String filename = getClass().getResource(TEST_FILE).getPath();
        System.setProperty(FILE_PROPERTY, filename);

        Properties properties = PropertiesLoader.load(FILE_PROPERTY, new File(defaultFile));

        assertThat(properties.getProperty("did.it.load")).isEqualTo("yes it did");
    }

    @Test
    public void readsDefaultFileIfPropertyNotSet() throws Exception {
        String defaultFile = getClass().getResource(DEFAULT_FILE).getPath();
        System.getProperties().remove(FILE_PROPERTY);

        Properties properties = PropertiesLoader.load(FILE_PROPERTY, new File(defaultFile));

        assertThat(properties.getProperty("did.it.load")).isEqualTo("no it didn't");
    }
}