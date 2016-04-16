package io.github.jonestimd.finance.swing.account;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountBuilder;
import io.github.jonestimd.finance.domain.account.AccountSummary;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.account.Company;
import io.github.jonestimd.finance.domain.event.AccountSummaryEvent;
import io.github.jonestimd.finance.swing.event.DomainEventPublisher;
import io.github.jonestimd.finance.swing.event.EventType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class AccountTableModelTest {
    private DomainEventPublisher domainEventPublisher = new DomainEventPublisher();
    private AccountTableModel model = new AccountTableModel(domainEventPublisher);
    private List<TableModelEvent> events = new ArrayList<TableModelEvent>();
    private TableModelListener listener = new TableModelListener() {
        public void tableChanged(TableModelEvent e) {
            events.add(e);
        }
    };
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

        assertEquals(2, events.size());
        assertEquals("company change event", 1, events.get(0).getColumn());
        assertEquals("company change event", 0, events.get(0).getFirstRow());
        assertEquals("company change event", 0, events.get(0).getLastRow());
        assertEquals("name change event", 2, events.get(1).getColumn());
        assertEquals("name change event", 0, events.get(1).getFirstRow());
        assertEquals("name change event", Integer.MAX_VALUE, events.get(1).getLastRow());
    }

    @Test
    public void testValidateAt() throws Exception {
        model.addRow(createAccount(null));
        model.addRow(createAccount("account name"));

        assertNull("closed", model.validateAt(0, 0));
        assertNull("company", model.validateAt(0, 1));
        assertEquals("name", "The account name must not be blank.", model.validateAt(0, 2));
        assertEquals("type", "The account type is required.", model.validateAt(0, 3));
        assertNull("description", model.validateAt(0, 4));

        model.setValueAt("account name2", 0, 2);
        assertNull(model.validateAt(0, 2));

        model.setValueAt("account name2", 1, 2);
        assertNull(model.validateAt(0, 2));
        assertEquals("The account name must be unique for the selected company.", model.validateAt(1, 2));

        model.setValueAt(new Company(), 0, 1);
        assertNull(model.validateAt(0, 2));
        assertNull(model.validateAt(1, 2));

        model.setValueAt(AccountType.BANK, 0, 3);
        assertNull(model.validateAt(0, 3));
    }

    @Test
    @Ignore // allow user to edit multiple accounts before saving
    public void testOnlyChangedAccountIsEditable() throws Exception {
        model.addRow(createAccount(null));
        model.addRow(createAccount("account 2"));
        model.commit();
        assertTrue(model.isCellEditable(0, 0));
        assertTrue(model.isCellEditable(0, 1));
        assertTrue(model.isCellEditable(0, 2));
        assertTrue(model.isCellEditable(0, 3));
        assertTrue(model.isCellEditable(0, 4));
        assertTrue(model.isCellEditable(1, 0));
        assertTrue(model.isCellEditable(1, 1));
        assertTrue(model.isCellEditable(1, 2));
        assertTrue(model.isCellEditable(1, 3));
        assertTrue(model.isCellEditable(1, 4));

        model.setValueAt("account 1", 0, 2);

        assertTrue(model.isCellEditable(0, 0));
        assertTrue(model.isCellEditable(0, 1));
        assertTrue(model.isCellEditable(0, 2));
        assertTrue(model.isCellEditable(0, 3));
        assertTrue(model.isCellEditable(0, 4));
        assertFalse(model.isCellEditable(1, 0));
        assertFalse(model.isCellEditable(1, 1));
        assertFalse(model.isCellEditable(1, 2));
        assertFalse(model.isCellEditable(1, 3));
        assertFalse(model.isCellEditable(1, 4));
    }

    private AccountSummary createAccount(String name) {
        Account account = new Account();
        account.setName(name);
        return new AccountSummary(account, 0L, BigDecimal.ZERO);
    }
}