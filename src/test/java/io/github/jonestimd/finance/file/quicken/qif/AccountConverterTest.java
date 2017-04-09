package io.github.jonestimd.finance.file.quicken.qif;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.account.AccountType;
import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.file.quicken.QuickenRecord;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static io.github.jonestimd.finance.domain.account.AccountType.*;
import static io.github.jonestimd.finance.file.quicken.qif.QifField.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class AccountConverterTest extends QifTestFixture {
    private AccountConverter converter;
    private AccountHolder accountHolder = new AccountHolder();
    private ArgumentCaptor<Account> saveCapture = ArgumentCaptor.forClass(Account.class);

    protected AccountType getAccountType() {
        return null;
    }

    @Test
    public void getTypesContainsAccounts() throws Exception {
        assertThat(new AccountConverter(null, null, null).getTypes().contains("Account")).isTrue();
    }

    @Test
    public void importRecordCopiesAccountFields() throws Exception {
        QifRecord record = new QifRecord(1L);
        record.setValue(NAME, "name");
        record.setValue(TYPE, "Bank");
        record.setValue(DESCRIPTION, "description");
        record.setValue(CREDIT_LIMIT, "credit limit");
        record.setValue(BALANCE_DATE, "balance date");
        record.setValue(BALANCE, "balance");
        record.setValue(QuickenRecord.END, "end or record");
        Currency currency = new Currency();
        when(assetOperations.getCurrency("USD")).thenReturn(currency);
        when(accountOperations.getAccount(null, "name")).thenReturn(null);
        stubSaveAccount();
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());
        converter = getQifContext().getAccountConverter();

        converter.importRecord(accountHolder, record);

        verify(accountOperations).save(saveCapture.capture());
        Account savedAccount = accountHolder.getAccount();
        assertThat(saveCapture.getValue()).isSameAs(savedAccount);
        assertThat(savedAccount.getCurrency()).isSameAs(currency);
        assertThat(savedAccount.getName()).isEqualTo(record.getValue(NAME));
        assertThat(savedAccount.getDescription()).isEqualTo(record.getValue(DESCRIPTION));
    }

    private void stubSaveAccount() {
        when(accountOperations.save(any(Account.class))).thenAnswer(new Answer<Account>() {
            public Account answer(InvocationOnMock invocation) throws Throwable {
                return (Account) invocation.getArguments()[0];
            }
        });
    }

    @Test
    public void importRecordSetsAccountType() throws Exception {
        Currency currency = new Currency();
        for (Map.Entry<AccountType, String> entry : createAccountTypeMap().entrySet()) {
            resetMocks();
            when(assetOperations.getCurrency("USD")).thenReturn(currency);
            when(accountOperations.getAccount(null, "name")).thenReturn(null);
            QifRecord record = new QifRecord(1L);
            record.setValue(NAME, "name");
            record.setValue(TYPE, entry.getValue());
            record.setValue(QuickenRecord.END, "end or record");
            stubSaveAccount();
            when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());
            converter = getQifContext().getAccountConverter();

            converter.importRecord(accountHolder, record);

            verify(accountOperations).save(saveCapture.capture());
            Account savedAccount = accountHolder.getAccount();
            assertThat(saveCapture.getValue()).isSameAs(savedAccount);
            assertThat(savedAccount.getCurrency()).isSameAs(currency);
            assertThat(savedAccount.getType()).isEqualTo(entry.getKey());
        }
    }

    private Map<AccountType, String> createAccountTypeMap() {
        Map<AccountType, String> map = new HashMap<AccountType, String>();
        map.put(BANK, "Bank");
        map.put(CREDIT, "CCard");
        map.put(CASH, "Cash");
        map.put(LOAN, "Oth L");
        map.put(BROKERAGE, "Port");
        map.put(_401K, "401(k)");
        return map;
    }

    @Test
    public void duplicateAccountNotSaved() throws Exception {
        String name = "name";
        when(accountOperations.getAccount(null, name)).thenReturn(new Account());
        QifRecord record = new QifRecord(1L);
        record.setValue(NAME, name);
        record.setValue(DESCRIPTION, "description");
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());
        converter = getQifContext().getAccountConverter();

        converter.importRecord(new AccountHolder(), record);

        verify(accountOperations).getAccount(null, name);
        verifyNoMoreInteractions(accountOperations);
    }

    @Test
    public void importRecordUpdatesAccountHolder() throws Exception {
        String name = "name";
        Account savedAccount = new Account();
        when(accountOperations.getAccount(null, name)).thenReturn(savedAccount);
        QifRecord record = new QifRecord(1L);
        record.setValue(NAME, name);
        when(payeeOperations.getAllPayees()).thenReturn(Collections.<Payee>emptyList());
        converter = getQifContext().getAccountConverter();

        converter.importRecord(accountHolder, record);

        assertThat(accountHolder.getAccount()).isSameAs(savedAccount);
    }
}