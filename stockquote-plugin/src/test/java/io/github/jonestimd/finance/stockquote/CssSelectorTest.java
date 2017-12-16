package io.github.jonestimd.finance.stockquote;

import java.io.IOException;
import java.util.Optional;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.*;

public class CssSelectorTest {
    @Test
    public void usesElementText() throws Exception {
        Config config = ConfigFactory.parseString("query = \"#quote > span:nth-child(2)\"");
        CssSelector selector = new CssSelector(config);

        Optional<String> value = selector.getValue(getElement());

        assertThat(value.get()).isEqualTo("25.00");
    }

    @Test
    public void returnsEmptyOptionForMissingElement() throws Exception {
        Config config = ConfigFactory.parseString("query = \"#quote > span:nth-child(3)\"");
        CssSelector selector = new CssSelector(config);

        Optional<String> value = selector.getValue(getElement());

        assertThat(value.isPresent()).isFalse();
    }

    @Test
    public void usesElementAttribute() throws Exception {
        Config config = ConfigFactory.parseString("query = \"#quote > meta[itemprop=price]\"\nattribute = content");
        CssSelector selector = new CssSelector(config);

        Optional<String> value = selector.getValue(getElement());

        assertThat(value.get()).isEqualTo("15.00");
    }

    @Test
    public void returnsEmptyStringForMissingAttribute() throws Exception {
        Config config = ConfigFactory.parseString("query = \"#quote > meta[itemprop=price]\"\nattribute = value");
        CssSelector selector = new CssSelector(config);

        Optional<String> value = selector.getValue(getElement());

        assertThat(value.get()).isEmpty();
    }

    private Element getElement() throws IOException {
        return Jsoup.parse(getClass().getResource("quote-S1.html").openStream(), "utf-8", "");
    }
}