package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;
import java.util.Collections;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.SecuritySummaryEvent;
import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import io.github.jonestimd.finance.plugin.SecurityTableExtension;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.finance.swing.transaction.TransactionSummaryColumnAdapter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SecurityTableModelTest {
    @Mock
    private SecurityTableExtension dataProvider;
    private DomainEventPublisher domainEventPublisher = new DomainEventPublisher();
    private SecurityTableModel model;

    private Security security1 = new SecurityBuilder().nextId().name("security1").get();
    private Account account1 = new AccountBuilder().nextId().company(new Company("comany")).name("account1").get();

    @Before
    public void createModel() {
        model = new SecurityTableModel(domainEventPublisher, Collections.singleton(dataProvider));
    }

    @Test
    public void noErrorForDomainEventsBeforeTableLoaded() throws Exception {
        domainEventPublisher.publishEvent(newEvent(new SecuritySummary(security1, 1L, BigDecimal.ONE, account1)));
    }

    @Test
    public void domainEventUpdatesRow() throws Exception {
        model.addRow(new SecuritySummary(security1, 1L, BigDecimal.ONE, null));
        SecuritySummary summary = new SecuritySummary(security1, 1L, BigDecimal.ONE, account1);

        domainEventPublisher.publishEvent(newEvent(summary));

        assertThat(model.getRowCount()).isEqualTo(1);
        assertThat(model.getRow(0).getAccount()).isNull();
        assertThat(model.getRow(0).getSecurity()).isSameAs(security1);
        assertThat(model.getRow(0).getShares()).isEqualTo(new BigDecimal(2));
        assertThat(model.getRow(0).getTransactionCount()).isEqualTo(2);
        verify(dataProvider).updateBean(model.getRow(0), TransactionSummaryColumnAdapter.COUNT_ADAPTER.getColumnId(), 1L);
        verify(dataProvider).updateBean(model.getRow(0), SecurityColumnAdapter.SHARES_ADAPTER.getColumnId(), BigDecimal.ONE);
    }

    private DomainEvent<Long, SecuritySummary> newEvent(SecuritySummary summary) {
        return new SecuritySummaryEvent(this, EventType.CHANGED, summary);
    }
}
