package io.github.jonestimd.finance.config;

import java.util.Collections;
import java.util.Optional;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class ConfigUtilsTest {
    @Test
    public void getReturnsOptionalWithValue() throws Exception {
        Config config = ConfigFactory.parseString("root { path = value }");

        Optional<Config> result = ConfigUtils.get(config, "root");

        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().root()).containsEntry("path", ConfigValueFactory.fromAnyRef("value"));
    }

    @Test
    public void getReturnsEmptyOptional() throws Exception {
        Config config = ConfigFactory.parseString("root { path = value }");

        Optional<Config> result = ConfigUtils.get(config, "notroot");

        assertThat(result.isPresent()).isFalse();
    }

    @Test
    public void getStringReturnsOptionalWithValue() throws Exception {
        Config config = ConfigFactory.parseString("root { path = value }");

        Optional<String> result = ConfigUtils.getString(config, "root.path");

        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isEqualTo("value");
    }

    @Test
    public void getStringReturnsEmptyOptional() throws Exception {
        Config config = ConfigFactory.parseString("root { path = value }");

        Optional<String> result = ConfigUtils.getString(config, "root.path2");

        assertThat(result.isPresent()).isFalse();
    }
}