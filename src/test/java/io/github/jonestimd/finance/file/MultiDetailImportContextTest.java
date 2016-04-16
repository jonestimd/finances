package io.github.jonestimd.finance.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFieldBuilder;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static io.github.jonestimd.finance.domain.fileimport.AmountFormat.*;
import static io.github.jonestimd.finance.domain.fileimport.FieldType.*;
import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MultiDetailImportContextTest {
    public static final String CATEGORY_NAME = "category name";
    public static final String ACCOUNT_NAME = "account name";
    @Mock
    private ImportFile importFile;
    @Mock
    private DomainMapper<Payee> payeeMapper;
    @Mock
    private DomainMapper<TransactionCategory> categoryMapper;
    @Mock
    private FieldValueExtractor fieldValueExtractor;

    private final InputStream inputStream = new ByteArrayInputStream("".getBytes());

    private final ImportField dateField = new ImportFieldBuilder().type(DATE).dateFormat("MM/dd/yyyy").get();
    private final ImportField payeeField = new ImportFieldBuilder().type(PAYEE).get();
    private final ImportField categoryField = new ImportFieldBuilder().type(CATEGORY).label(CATEGORY_NAME).amountFormat(FIXED).get();
    private final ImportField transferField = new ImportFieldBuilder().type(TRANSFER_ACCOUNT).label(ACCOUNT_NAME).amountFormat(FIXED).get();

    @Before
    public void trainImportFile() throws Exception {
        when(importFile.isMultiDetail()).thenReturn(true);
        when(importFile.getFieldValueExtractor()).thenReturn(fieldValueExtractor);
    }

    @Test
    public void setsDateOnTransaction() throws Exception {
        final Payee payee = new Payee();
        when(fieldValueExtractor.parse(same(inputStream))).thenReturn(ImmutableList.of(ImmutableMap.of(dateField, "03/10/2012")));

        List<Transaction> transactions = new MultiDetailImportContext(importFile, payeeMapper, categoryMapper).parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDate()).isEqualTo(new SimpleDateFormat("MM/dd/yyyy").parse("03/10/2012"));
    }

    @Test
    public void setsPayeeOnTransaction() throws Exception {
        final Payee payee = new Payee();
        when(fieldValueExtractor.parse(same(inputStream))).thenReturn(ImmutableList.of(
                ImmutableMap.of(payeeField, "payee name")));
        when(payeeMapper.get("payee name")).thenReturn(payee);

        List<Transaction> transactions = new MultiDetailImportContext(importFile, payeeMapper, categoryMapper).parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getPayee()).isSameAs(payee);
    }

    @Test
    public void addsDetailWithCategory() throws Exception {
        final TransactionCategory category = new TransactionCategory();
        when(fieldValueExtractor.parse(same(inputStream))).thenReturn(ImmutableList.of(
                ImmutableMap.of(categoryField, "123456")));
        when(categoryMapper.get(CATEGORY_NAME)).thenReturn(category);

        List<Transaction> transactions = new MultiDetailImportContext(importFile, payeeMapper, categoryMapper).parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isSameAs(category);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().compareTo(new BigDecimal("1234.56"))).isEqualTo(0);
    }

    @Test
    public void addsDetailWithTransfer() throws Exception {
        final Account acount = new Account();
        when(fieldValueExtractor.parse(same(inputStream))).thenReturn(ImmutableList.of(
                ImmutableMap.of(transferField, "123456")));
        when(importFile.getTransferAccount(ACCOUNT_NAME)).thenReturn(acount);

        List<Transaction> transactions = new MultiDetailImportContext(importFile, payeeMapper, categoryMapper).parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getRelatedDetail().getTransaction().getAccount()).isSameAs(acount);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().compareTo(new BigDecimal("1234.56"))).isEqualTo(0);
    }
}