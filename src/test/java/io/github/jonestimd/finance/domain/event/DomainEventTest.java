package io.github.jonestimd.finance.domain.event;

import java.math.BigDecimal;
import java.util.Arrays;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import io.github.jonestimd.finance.swing.event.EventType;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class DomainEventTest {
    @Test(expected = IllegalArgumentException.class)
    public void getDomainObjectByIdThrowsExceptionForMultipleObjects() throws Exception {
        Security security = new SecurityBuilder().nextId().name("security").get();
        SecuritySummary summary1 = new SecuritySummary(security, 0L, BigDecimal.ONE);
        SecuritySummary summary2 = new SecuritySummary(security, 0L, BigDecimal.ONE);

        new DomainEvent<>(this, EventType.CHANGED, Arrays.asList(summary1, summary2), SecuritySummary.class)
            .getDomainObject(security.getId());
    }

    @Test
    public void createReloadEvent() throws Exception {
        DomainEvent<Object, UniqueId<Object>> event = new DomainEvent<>("source");

        assertThat(event.isReload()).isTrue();
        assertThat(event.getSource()).isEqualTo("source");
        assertThat(event.getDomainClass()).isEqualTo(null);
        assertThat(event.getDomainObjects()).isEmpty();
        assertThat(event.getReplacement()).isNull();
    }
}
