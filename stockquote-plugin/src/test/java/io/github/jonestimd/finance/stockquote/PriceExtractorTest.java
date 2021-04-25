package io.github.jonestimd.finance.stockquote;

import java.math.BigDecimal;
import java.util.Optional;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jsoup.nodes.Element;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PriceExtractorTest {
    @Mock
    private CssSelector selector;
    @Mock
    private Element element;

    @Test
    public void returnsNullForMissingValue() throws Exception {
        when(selector.getValue(element)).thenReturn(Optional.empty());
        PriceExtractor extractor = new PriceExtractor(ConfigFactory.empty(), selector);

        BigDecimal value = extractor.getValue(element);

        assertThat(value).isNull();
    }

    @Test
    public void usesDefaultFormatIfNoFormatConfigured() throws Exception {
        when(selector.getValue(element)).thenReturn(Optional.of("1,234.56"));
        PriceExtractor extractor = new PriceExtractor(ConfigFactory.empty(), selector);

        BigDecimal value = extractor.getValue(element);

        assertThat(value).isEqualTo(new BigDecimal("1234.56"));
    }

    @Test
    public void throwsExceptionIfTextNotFullyParsed() throws Exception {
        when(selector.getValue(element)).thenReturn(Optional.of("1,234.56"));
        Config config = ConfigFactory.parseString("format = \"###0.0#\"");
        PriceExtractor extractor = new PriceExtractor(config, selector);

        try {
            extractor.getValue(element);
            fail("expected an exception");
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).isEqualTo("Unexpected trailing text: 1,234.56 at ,");
        }
    }
}