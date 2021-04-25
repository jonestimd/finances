package io.github.jonestimd.finance.stockquote;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.service.ServiceLocator;
import io.github.jonestimd.finance.swing.event.DomainEventListener;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StockQuotePluginTest {
    @Mock
    private ServiceLocator serviceLocator;
    @Mock
    private DomainEventPublisher domainEventPublisher;
    private final StockQuotePlugin plugin = new StockQuotePlugin();

    @Test
    public void getSecurityTableExtensionsNoneEnabled() throws Exception {
        Config config = ConfigFactory.load();

        plugin.initialize(config, serviceLocator, domainEventPublisher);

        assertThat(plugin.getSecurityTableExtensions()).isEmpty();
        verifyNoInteractions(serviceLocator, domainEventPublisher);
    }

    @Test
    public void getSecurityTableExtensionsOneEnabled() throws Exception {
        Config config = ConfigFactory.load("io/github/jonestimd/finance/stockquote/enabled.conf");

        plugin.initialize(config, serviceLocator, domainEventPublisher);

        assertThat(plugin.getSecurityTableExtensions()).hasSize(1);
        assertThat(plugin.getSecurityTableExtensions().get(0)).isInstanceOf(StockQuoteTableProvider.class);
        verify(domainEventPublisher).register(same(SecuritySummary.class), isA(DomainEventListener.class));
        verify(domainEventPublisher).register(same(Security.class), isA(DomainEventListener.class));
        verifyNoMoreInteractions(domainEventPublisher);
        verifyNoInteractions(serviceLocator);
    }
}