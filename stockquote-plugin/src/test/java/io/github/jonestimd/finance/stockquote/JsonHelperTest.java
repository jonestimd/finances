package io.github.jonestimd.finance.stockquote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;

import org.junit.Test;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

public class JsonHelperTest {
    private final JsonProvider provider = JsonProvider.provider();

    @Test
    public void getStringReturnsRootValue() throws Exception {
        JsonObject jsonObject = createBuilder("root", "value").build();

        String value = new JsonHelper(getStream(jsonObject)).getString(singletonList("root"));

        assertThat(value).isEqualTo("value");
    }

    @Test
    public void getStringReturnsNestedValue() throws Exception {
        JsonObject jsonObject = createBuilder("root", createBuilder("nested", "value")).build();

        String value = new JsonHelper(getStream(jsonObject)).getString(Arrays.asList("root", "nested"));

        assertThat(value).isEqualTo("value");
    }

    @Test
    public void asBigDecimal() throws Exception {
        BigDecimal expected = new BigDecimal("123.45");
        JsonObject jsonObject = createBuilder("root", expected).build();

        BigDecimal value = JsonHelper.asBigDecimal(jsonObject.get("root"));

        assertThat(value).isEqualByComparingTo(expected);
    }

    @Test
    public void getArray() throws Exception {
        List<String> expected = Arrays.asList("one", "two");
        JsonObject jsonObject = createBuilder("root", expected).build();

        JsonArray values = new JsonHelper(getStream(jsonObject)).getArray(singletonList("root"));

        assertThat(values.stream().map(v -> ((JsonString) v).getString())).containsExactlyElementsOf(expected);
    }

    @Test
    public void optionalStringReturnsEmptyForMissingValue() throws Exception {
        JsonObject jsonObject = createBuilder("root", "value").build();

        Optional<String> value = new JsonHelper(getStream(jsonObject)).optionalString("other");

        assertThat(value.isPresent()).isFalse();
    }

    @Test
    public void optionalStringReturnsValue() throws Exception {
        JsonObject jsonObject = createBuilder("root", "value").build();

        Optional<String> value = new JsonHelper(getStream(jsonObject)).optionalString("root");

        assertThat(value.isPresent()).isTrue();
        assertThat(value.get()).isEqualTo("value");
    }

    @Test
    public void optionalStringReturnsArrayElement() throws Exception {
        JsonArray jsonArray = provider.createArrayBuilder().add("value").build();

        Optional<String> value = new JsonHelper(getStream(jsonArray)).optionalString("0");

        assertThat(value.isPresent()).isTrue();
        assertThat(value.get()).isEqualTo("value");
    }

    @Test
    public void optionalStringReturnsEmptyForIndexOutOfBounds() throws Exception {
        JsonArray jsonArray = provider.createArrayBuilder().add("value").build();

        JsonHelper jsonHelper = new JsonHelper(getStream(jsonArray));

        assertThat(jsonHelper.optionalString("1").isPresent()).isFalse();
        assertThat(jsonHelper.optionalString("-1").isPresent()).isFalse();
    }

    @Test
    public void findValueReturnsValueForMatchingKey() throws Exception {
        JsonObject jsonObject = createBuilder("1. root", "value").add("2. other", "not it").build();

        JsonValue value = new JsonHelper(getStream(jsonObject)).findValue(singletonList("\\d\\. root"));

        assertThat(value).isInstanceOf(JsonString.class);
        assertThat(((JsonString) value).getString()).isEqualTo("value");
    }

    @Test
    public void findStringReturnsStringForMatchingKeys() throws Exception {
        JsonObject jsonObject = createBuilder("1. root", "value").add("2. other", "not it").build();

        String value = new JsonHelper(getStream(jsonObject)).findString(singletonList("\\d\\. root"));

        assertThat(value).isEqualTo("value");
    }

    @Test
    public void findStringReturnsStringForMatchingKey() throws Exception {
        JsonObject jsonObject = createBuilder("1. root", "value").add("2. other", "not it").build();

        String value = JsonHelper.findString("\\d\\. root", jsonObject);

        assertThat(value).isEqualTo("value");
    }

    @Test
    public void findObjectReturnsValueForMatchingKey() throws Exception {
        JsonObject jsonObject = createBuilder("1. root", createBuilder("nested", "value")).add("2. other", "not it").build();

        JsonValue value = new JsonHelper(getStream(jsonObject)).findObject(singletonList("\\d\\. root"));

        assertThat(value).isInstanceOf(JsonObject.class);
        assertThat(((JsonObject) value).getString("nested")).isEqualTo("value");
    }

    @Test
    public void mapEntries() throws Exception {
        JsonObject jsonObject = provider.createObjectBuilder()
                .add("p1", createBuilder("child", new BigDecimal("123.45")))
                .add("p2", createBuilder("child", new BigDecimal("234.56"))).build();

        Map<String, BigDecimal> map = new JsonHelper(getStream(jsonObject)).mapEntries(singletonList("child"), JsonHelper::asBigDecimal);

        assertThat(map).containsEntry("p1", new BigDecimal("123.45"));
        assertThat(map).containsEntry("p2", new BigDecimal("234.56"));
    }

    @Test
    public void toMap() throws Exception {
        JsonArray jsonArray = provider.createArrayBuilder()
                .add(provider.createObjectBuilder().add("name", "p1").add("value", new BigDecimal("123.45")))
                .add(provider.createObjectBuilder().add("name", "p2").add("value", new BigDecimal("234.56"))).build();

        Map<String, BigDecimal> map = new JsonHelper(getStream(jsonArray))
                .toMap(singletonList("name"), singletonList("value"), JsonHelper::asBigDecimal);

        assertThat(map).containsEntry("p1", new BigDecimal("123.45"));
        assertThat(map).containsEntry("p2", new BigDecimal("234.56"));
    }

    private JsonObjectBuilder createBuilder(String name, String value) {
        return provider.createObjectBuilder().add(name, value);
    }

    private JsonObjectBuilder createBuilder(String name, BigDecimal value) {
        return provider.createObjectBuilder().add(name, value);
    }

    private JsonObjectBuilder createBuilder(String name, JsonObjectBuilder value) {
        return provider.createObjectBuilder().add(name, value);
    }

    private JsonObjectBuilder createBuilder(String name, List<String> values) {
        JsonArrayBuilder arrayBuilder = provider.createArrayBuilder();
        values.forEach(arrayBuilder::add);
        return provider.createObjectBuilder().add(name, arrayBuilder);
    }

    private InputStream getStream(JsonStructure json) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        provider.createWriter(outputStream).write(json);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}