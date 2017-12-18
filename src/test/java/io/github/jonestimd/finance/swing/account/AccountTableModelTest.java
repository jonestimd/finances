package io.github.jonestimd.finance.swing.account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.UniqueId;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.event.AccountSummaryEvent;
import io.github.jonestimd.finance.domain.event.CompanyEvent;
import io.github.jonestimd.finance.domain.event.DomainEvent;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class AccountTableModelTest {
    private DomainEventPublisher domainEventPublisher = new DomainEventPublisher();
    private AccountTableModel model = new AccountTableModel(domainEventPublisher);
    private List<TableModelEvent> events = new ArrayList<>();
    private TableModelListener listener = event -> events.add(event);
    private Account account1 = new AccountBuilder().nextId().name("account1").get();

    @Before
    public void setUp() {
        model.addTableModelListener(listener);
    }

    @Test
    public void noErrorForDomainEventsBeforeTableLoaded() throws Exception {
        domainEventPublisher.publishEvent(new AccountSummaryEvent(this, EventType.CHANGED, new AccountSummary(account1, 1L, BigDecimal.ONE)));
    }

    @Test
    public void testUpdateCompanyFiresChangeForNames() throws Exception {
        model.addRow(createAccount(null));
        events.clear();

        model.setValueAt(new Company(), 0, 1);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getColumn()).as("company change event").isEqualTo(1);
        assertThat(events.get(0).getFirstRow()).as("company change event").isEqualTo(0);
        assertThat(events.get(0).getLastRow()).as("company change event").isEqualTo(0);
        assertThat(events.get(1).getColumn()).as("name change event").isEqualTo(2);
        assertThat(events.get(1).getFirstRow()).as("name change event").isEqualTo(0);
        assertThat(events.get(1).getLastRow()).as("name change event").isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    public void testValidateAt() throws Exception {
        model.addRow(createAccount(null));
        model.addRow(createAccount("account name"));

        assertThat(model.validateAt(0, 0)).as("closed").isNull();
        assertThat(model.validateAt(0, 1)).as("company").isNull();
        assertThat(model.validateAt(0, 2)).as("name").isEqualTo("The account name must not be blank.");
        assertThat(model.validateAt(0, 3)).as("type").isEqualTo("The account type is required.");
        assertThat(model.validateAt(0, 4)).as("description").isNull();

        model.setValueAt("account name2", 0, 2);
        assertThat(model.validateAt(0, 2)).isNull();

        model.setValueAt("account name2", 1, 2);
        assertThat(model.validateAt(0, 2)).isNull();
        assertThat(model.validateAt(1, 2)).isEqualTo("The account name must be unique for the selected company.");

        model.setValueAt(new Company(), 0, 1);
        assertThat(model.validateAt(0, 2)).isNull();
        assertThat(model.validateAt(1, 2)).isNull();

        model.setValueAt(AccountType.BANK, 0, 3);
        assertThat(model.validateAt(0, 3)).isNull();
    }

    @Test
    @Ignore // allow user to edit multiple accounts before saving
    public void testOnlyChangedAccountIsEditable() throws Exception {
        model.addRow(createAccount(null));
        model.addRow(createAccount("account 2"));
        model.commit();
        assertThat(model.isCellEditable(0, 0)).isTrue();
        assertThat(model.isCellEditable(0, 1)).isTrue();
        assertThat(model.isCellEditable(0, 2)).isTrue();
        assertThat(model.isCellEditable(0, 3)).isTrue();
        assertThat(model.isCellEditable(0, 4)).isTrue();
        assertThat(model.isCellEditable(1, 0)).isTrue();
        assertThat(model.isCellEditable(1, 1)).isTrue();
        assertThat(model.isCellEditable(1, 2)).isTrue();
        assertThat(model.isCellEditable(1, 3)).isTrue();
        assertThat(model.isCellEditable(1, 4)).isTrue();

        model.setValueAt("account 1", 0, 2);

        assertThat(model.isCellEditable(0, 0)).isTrue();
        assertThat(model.isCellEditable(0, 1)).isTrue();
        assertThat(model.isCellEditable(0, 2)).isTrue();
        assertThat(model.isCellEditable(0, 3)).isTrue();
        assertThat(model.isCellEditable(0, 4)).isTrue();
        assertThat(model.isCellEditable(1, 0)).isFalse();
        assertThat(model.isCellEditable(1, 1)).isFalse();
        assertThat(model.isCellEditable(1, 2)).isFalse();
        assertThat(model.isCellEditable(1, 3)).isFalse();
        assertThat(model.isCellEditable(1, 4)).isFalse();
    }

    @Test
    public void addCompanyDomainEventDoesNothing() throws Exception {
        Company company = TestDomainUtils.setId(new Company("the company"));
        model.addRow(createAccount("another account"));
        events.clear();

        domainEventPublisher.publishEvent(new CompanyEvent("test", EventType.ADDED, Collections.singleton(company)));

        assertThat(events).isEmpty();
    }

    @Test
    public void changeCompanyDomainEventUpdatesNames() throws Exception {
        Company company = TestDomainUtils.setId(new Company("the company"));
        Account account = new Account(company, "the account");
        model.addRow(new AccountSummary(account, 0L, BigDecimal.ZERO));
        model.addRow(createAccount("another account"));
        events.clear();

        domainEventPublisher.publishEvent(new CompanyEvent("test", EventType.CHANGED, Collections.singleton(company)));

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getType()).isEqualTo(TableModelEvent.UPDATE);
        assertThat(events.get(0).getFirstRow()).isEqualTo(0);
        assertThat(events.get(0).getLastRow()).isEqualTo(0);
        assertThat(events.get(0).getColumn()).isEqualTo(AccountTableModel.COMPANY_INDEX);
    }

    private AccountSummary createAccount(String name) {
        Account account = new Account();
        account.setName(name);
        return new AccountSummary(account, 0L, BigDecimal.ZERO);
    }
}