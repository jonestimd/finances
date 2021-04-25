package io.github.jonestimd.finance.file;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.fileimport.ImportField;
import io.github.jonestimd.finance.domain.fileimport.ImportFieldBuilder;
import io.github.jonestimd.finance.domain.fileimport.ImportFile;
import io.github.jonestimd.finance.domain.transaction.Payee;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionCategory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static io.github.jonestimd.finance.domain.fileimport.AmountFormat.*;
import static io.github.jonestimd.finance.domain.fileimport.FieldType.*;
import static org.assertj.core.api.Assertions.*;
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
    private DomainMapper<Security> securityMapper;

    private final InputStream inputStream = new ByteArrayInputStream("".getBytes());

    private final ImportField dateField = new ImportFieldBuilder().type(DATE).get();
    private final ImportField payeeField = new ImportFieldBuilder().type(PAYEE).get();
    private final ImportField transferField = new ImportFieldBuilder().type(TRANSFER_ACCOUNT).label(ACCOUNT_NAME)
            .amountFormat(FIXED).account(new Account()).get();

    public void trainImportFile(Iterable<ListMultimap<ImportField, String>> rows) throws Exception {
        when(importFile.parse(inputStream)).thenReturn(rows);
    }

    @Test
    public void setsDateOnTransaction() throws Exception {
        final Date date = new Date();
        when(importFile.parseDate(anyString())).thenReturn(date);
        trainImportFile(ImmutableList.of(ImmutableListMultimap.of(dateField, "03/10/2012")));

        List<Transaction> transactions = new MultiDetailImportContext(importFile, payeeMapper, securityMapper).parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDate()).isSameAs(date);
        verify(importFile).parseDate("03/10/2012");
    }

    @Test
    public void setsPayeeOnTransaction() throws Exception {
        final Payee payee = new Payee();
        trainImportFile(ImmutableList.of(ImmutableListMultimap.of(payeeField, "payee name")));
        when(payeeMapper.get("payee name")).thenReturn(payee);

        List<Transaction> transactions = new MultiDetailImportContext(importFile, payeeMapper, securityMapper).parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getPayee()).isSameAs(payee);
    }

    @Test
    public void addsDetailWithCategory() throws Exception {
        final TransactionCategory category = new TransactionCategory();
        final ImportField categoryField = new ImportFieldBuilder().type(CATEGORY)
                .label(CATEGORY_NAME).amountFormat(FIXED).category(category).get();
        trainImportFile(ImmutableList.of(ImmutableListMultimap.of(categoryField, "123456")));

        List<Transaction> transactions = new MultiDetailImportContext(importFile, payeeMapper, securityMapper).parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isSameAs(category);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().compareTo(new BigDecimal("1234.56"))).isEqualTo(0);
    }

    @Test
    public void addsDetailWithTransfer() throws Exception {
        trainImportFile(ImmutableList.of(ImmutableListMultimap.of(transferField, "123456")));

        List<Transaction> transactions = new MultiDetailImportContext(importFile, payeeMapper, securityMapper).parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getRelatedDetail().getTransaction().getAccount())
                .isSameAs(transferField.getTransferAccount());
        assertThat(transactions.get(0).getDetails().get(0).getAmount().compareTo(new BigDecimal("1234.56"))).isEqualTo(0);
    }

    @Test
    public void removesZeroAmountDetails() throws Exception {
        trainImportFile(ImmutableList.of(ImmutableListMultimap.of(transferField, "0.00")));

        List<Transaction> transactions = new MultiDetailImportContext(importFile, payeeMapper, securityMapper).parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).isEmpty();
    }
}