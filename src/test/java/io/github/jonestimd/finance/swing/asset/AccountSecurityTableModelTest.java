package io.github.jonestimd.finance.swing.asset;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.account.CompanyBuilder;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.event.AccountEvent;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.domain.event.SecurityEvent;
import io.github.jonestimd.finance.domain.event.SecuritySummaryEvent;
import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import io.github.jonestimd.swing.table.model.TableDataProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.github.jonestimd.mockito.MockitoHelper.matches;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccountSecurityTableModelTest {
    @Mock
    private TableDataProvider<SecuritySummary> dataProvider;
    @Mock
    private TableModelListener modelListener;
    private DomainEventPublisher domainEventPublisher = new DomainEventPublisher();
    private AccountSecurityTableModel model;
    private Security security1 = new SecurityBuilder().nextId().name("security1").get();
    private Security security2 = new SecurityBuilder().nextId().name("security2").get();
    private Company company = new CompanyBuilder().nextId().name("company").get();
    private Account account1 = new AccountBuilder().nextId().company(company).name("account1").get();

    @Before
    public void createModel() {
        model = new AccountSecurityTableModel(domainEventPublisher, Collections.singleton(dataProvider));
        model.addTableModelListener(modelListener);
    }

    @Test
    public void notEditable() throws Exception {
        model.put(account1.getId(), new SecuritySummary(security1, 1L, BigDecimal.ONE, account1));
        for (int rowIndex = 0; rowIndex < model.getRowCount(); rowIndex++) {
            for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++) {
                assertThat(model.isCellEditable(rowIndex, columnIndex)).isFalse();
            }
        }
    }

    @Test
    public void noErrorForDomainEventsBeforeTableLoaded() throws Exception {
        domainEventPublisher.publishEvent(newEvent(new SecuritySummary(security1, 1L, BigDecimal.ONE, account1)));
    }

    @Test
    public void domainEventAddsRow() throws Exception {
        SecuritySummary summary = new SecuritySummary(security1, 1L, BigDecimal.ONE, account1);

        domainEventPublisher.publishEvent(newEvent(summary));

        assertThat(model.getRowCount()).isEqualTo(2);
        assertThat(model.isSectionRow(0)).isTrue();
        assertThat(model.getSectionName(0)).isEqualTo(account1.qualifiedName(": "));
        assertThat(model.getBeans()).containsOnly(summary);
        verify(dataProvider).addBean(summary);
    }

    @Test
    public void domainEventUpdatesRow() throws Exception {
        model.put(account1.getId(), new SecuritySummary(security1, 1L, BigDecimal.ONE, account1));
        SecuritySummary summary = new SecuritySummary(security1, 1L, BigDecimal.ONE, account1);

        domainEventPublisher.publishEvent(newEvent(summary));

        assertThat(model.getRowCount()).isEqualTo(2);
        assertThat(model.getBean(1).getAccount()).isSameAs(account1);
        assertThat(model.getBean(1).getSecurity()).isSameAs(security1);
        assertThat(model.getBean(1).getShares()).isEqualTo(new BigDecimal(2));
        verify(dataProvider).updateBean(model.getBean(1), SecurityColumnAdapter.SHARES_ADAPTER.getColumnId(), BigDecimal.ONE);
    }

    @Test
    public void domainEventRemoveRowWithNoTransactions() throws Exception {
        model.put(account1.getId(), new SecuritySummary(security1, 1L, BigDecimal.ONE, account1));
        model.put(account1.getId(), new SecuritySummary(security2, 1L, BigDecimal.ONE, account1));
        SecuritySummary summary = new SecuritySummary(security1, -1L, BigDecimal.ONE.negate(), account1);

        domainEventPublisher.publishEvent(newEvent(summary));

        assertThat(model.getRowCount()).isEqualTo(2);
        assertThat(model.getBean(1).getAccount()).isSameAs(account1);
        assertThat(model.getBean(1).getSecurity()).isSameAs(security2);
        assertThat(model.getBean(1).getShares()).isEqualTo(BigDecimal.ONE);
        assertThat(model.getBean(1).getTransactionCount()).isEqualTo(1);
        ArgumentCaptor<SecuritySummary> captor = ArgumentCaptor.forClass(SecuritySummary.class);
        verify(dataProvider).removeBean(captor.capture());
        assertThat(captor.getValue().getAccount()).isSameAs(account1);
        assertThat(captor.getValue().getSecurity()).isSameAs(security1);
    }

    @Test
    public void securityDomainEventUpdatesRows() throws Exception {
        Account account2 = new AccountBuilder().nextId().company(company).name("account2").get();
        model.put(account1.getId(), new SecuritySummary(security1, 1L, BigDecimal.ONE, account1));
        model.put(account1.getId(), new SecuritySummary(security2, 1L, BigDecimal.ONE, account1));
        model.put(account2.getId(), new SecuritySummary(security1, 1L, BigDecimal.ONE, account2));
        reset(modelListener);

        Security updated = new SecurityBuilder(security1).name("new Name").get();
        domainEventPublisher.publishEvent(new SecurityEvent(this, EventType.CHANGED, updated));

        assertThat(model.getBean(1).getSecurity()).isSameAs(updated);
        assertThat(model.getBean(4).getSecurity()).isSameAs(updated);
        verify(modelListener).tableChanged(matches(new TableModelEvent(model, 1, 1, TableModelEvent.ALL_COLUMNS)));
        verify(modelListener).tableChanged(matches(new TableModelEvent(model, 4, 4, TableModelEvent.ALL_COLUMNS)));
    }

    @Test
    public void accountDomainEventUpdatesGroup() throws Exception {
        Account account2 = new AccountBuilder().nextId().company(company).name("account2").get();
        SecuritySummary summary1 = new SecuritySummary(security1, 1L, BigDecimal.ONE, account1);
        SecuritySummary summary2 = new SecuritySummary(security2, 1L, BigDecimal.ONE, account1);
        model.put(account1.getId(), summary1);
        model.put(account1.getId(), summary2);
        model.put(account2.getId(), new SecuritySummary(security1, 1L, BigDecimal.ONE, account2));
        reset(modelListener);

        Account updated = new AccountBuilder(account1).name("new Name").get();
        domainEventPublisher.publishEvent(new AccountEvent(this, EventType.CHANGED, updated));

        assertThat(model.getSectionName(2)).isEqualTo(updated.qualifiedName(": "));
        assertThat(model.getBeans(updated.getId())).containsOnly(summary1, summary2);
        for (SecuritySummary summary : model.getBeans(updated.getId())) {
            assertThat(summary.getAccount()).isSameAs(updated);
        }
        verify(modelListener).tableChanged(matches(new TableModelEvent(model, 0, 2, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE)));
        verify(modelListener).tableChanged(matches(new TableModelEvent(model, 2, 4, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT)));
    }

    @Test
    public void companyDomainEventUpdatesGroup() throws Exception {
        Account account2 = new AccountBuilder().nextId().company(company).name("account2").get();
        SecuritySummary summary1 = new SecuritySummary(security1, 1L, BigDecimal.ONE, account1);
        SecuritySummary summary2 = new SecuritySummary(security2, 1L, BigDecimal.ONE, account1);
        SecuritySummary summary3 = new SecuritySummary(security1, 1L, BigDecimal.ONE, account2);
        model.put(account1.getId(), summary1);
        model.put(account1.getId(), summary2);
        model.put(account2.getId(), summary3);
        reset(modelListener);

        Company updated = new CompanyBuilder(company).name("new name").get();
        domainEventPublisher.publishEvent(new DomainEvent<>(this, EventType.CHANGED, updated, Company.class));

        assertThat(account1.getCompany()).isSameAs(updated);
        assertThat(account2.getCompany()).isSameAs(updated);
        assertThat(model.getSectionName(0)).isEqualTo(account1.qualifiedName(": "));
        assertThat(model.getSectionName(3)).isEqualTo(account2.qualifiedName(": "));
        verify(modelListener).tableChanged(matches(new TableModelEvent(model, 0, 2, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE)));
        verify(modelListener).tableChanged(matches(new TableModelEvent(model, 2, 4, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT)));
        verify(modelListener).tableChanged(matches(new TableModelEvent(model, 0, 1, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE)));
        verify(modelListener).tableChanged(matches(new TableModelEvent(model, 3, 4, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT)));
    }

    @Test
    public void setBeansUpdatesAccountMap() throws Exception {
        Account account2 = new AccountBuilder().nextId().company(company).name("account2").get();
        SecuritySummary summary1 = new SecuritySummary(security1, 1L, BigDecimal.ONE, account1);
        SecuritySummary summary2 = new SecuritySummary(security2, 1L, BigDecimal.ONE, account1);
        SecuritySummary summary3 = new SecuritySummary(security1, 1L, BigDecimal.ONE, account2);

        model.setBeans(Arrays.asList(summary1, summary2, summary3));

        assertThat(model.getSectionName(0)).isEqualTo(account1.qualifiedName(": "));
        assertThat(model.getSectionName(3)).isEqualTo(account2.qualifiedName(": "));
    }

    @Test
    public void updateBeansUpdatesAccountMap() throws Exception {
        Account account2 = new AccountBuilder().nextId().company(company).name("account2").get();
        Account account2b = new AccountBuilder().id(account2.getId()).company(company).name("account2 updated").get();
        SecuritySummary summary1 = new SecuritySummary(security1, 1L, BigDecimal.ONE, account1);
        SecuritySummary summary2 = new SecuritySummary(security2, 1L, BigDecimal.ONE, account1);
        SecuritySummary summary3 = new SecuritySummary(security1, 1L, BigDecimal.ONE, account2);
        SecuritySummary summary3b = new SecuritySummary(security1, 1L, BigDecimal.ONE, account2b);
        model.setBeans(Arrays.asList(summary1, summary2, summary3));

        model.updateBeans(singletonList(summary3b), SecuritySummary::isSameIds);

        assertThat(model.getSectionName(0)).isEqualTo(account1.qualifiedName(": "));
        assertThat(model.getSectionName(3)).isEqualTo(account2b.qualifiedName(": "));
    }

    private DomainEvent<Long, SecuritySummary> newEvent(SecuritySummary summary) {
        return new SecuritySummaryEvent(this, EventType.CHANGED, summary);
    }
}
