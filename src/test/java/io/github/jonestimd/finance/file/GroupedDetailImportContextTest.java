package io.github.jonestimd.finance.file;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ImmutableListMultimap;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.fileimport.AmountFormat;
import io.github.jonestimd.finance.domain.fileimport.FieldType;
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

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GroupedDetailImportContextTest {
    public static final String CATEGORY_CODE = "category code";
    public static final String PAYEE_NAME = "Payee name";
    public static final String TRANSFER_ACCOUNT = "Transfer account";
    public static final String SECURITY_NAME = "Security name";
    @Mock
    private ImportFile importFile;
    @Mock
    private DomainMapper<Payee> payeeMapper;
    @Mock
    private DomainMapper<Security> securityMapper;
    @Mock
    private DomainMapper<TransactionCategory> categoryMapper;

    private final ImportField dateField = new ImportFieldBuilder().type(FieldType.DATE).get();
    private final ImportField payeeField = new ImportFieldBuilder().type(FieldType.PAYEE).get();
    private final ImportField securityField = new ImportFieldBuilder().type(FieldType.SECURITY).get();
    private final ImportField amountField = new ImportFieldBuilder().type(FieldType.AMOUNT).amountFormat(AmountFormat.DECIMAL).get();
    private final ImportField sharesField = new ImportFieldBuilder().type(FieldType.ASSET_QUANTITY).amountFormat(AmountFormat.DECIMAL).get();
    private final ImportField categoryField = new ImportFieldBuilder().type(FieldType.CATEGORY).get();
    private final ImportField transferField = new ImportFieldBuilder().type(FieldType.TRANSFER_ACCOUNT).get();

    @Test
    public void populatesTransaction() throws Exception {
        final Date date = new Date();
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final Payee payee = new Payee();
        final Account account = new Account();
        Security security = new Security();
        when(importFile.parseDate(anyString())).thenReturn(date);
        when(importFile.getAccount()).thenReturn(account);
        when(importFile.parse(inputStream)).thenReturn(singletonList(
                ImmutableListMultimap.of(dateField, "01/15/1990", payeeField, PAYEE_NAME, securityField, SECURITY_NAME)));
        ImportContext context = new GroupedDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);
        when(payeeMapper.get(PAYEE_NAME)).thenReturn(payee);
        when(securityMapper.get(SECURITY_NAME)).thenReturn(security);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAccount()).isSameAs(account);
        assertThat(transactions.get(0).getDate()).isSameAs(date);
        assertThat(transactions.get(0).getPayee()).isSameAs(payee);
        assertThat(transactions.get(0).getSecurity()).isSameAs(security);
        verify(importFile).parseDate("01/15/1990");
    }

    @Test
    public void populatesDetail() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TransactionCategory category = new TransactionCategory();
        when(importFile.parse(inputStream)).thenReturn(singletonList(
                ImmutableListMultimap.of(amountField, "10", categoryField, CATEGORY_CODE, sharesField, "1")));
        ImportContext context = new GroupedDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);
        when(categoryMapper.get(CATEGORY_CODE)).thenReturn(category);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isSameAs(category);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().toString()).isEqualTo("10");
        assertThat(transactions.get(0).getDetails().get(0).getAssetQuantity().toString()).isEqualTo("1");
    }

    @Test
    public void groupsRowsByTransaction() throws Exception {
        String otherCategory = "other category";
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TransactionCategory category1 = new TransactionCategory(CATEGORY_CODE);
        final TransactionCategory category2 = new TransactionCategory(otherCategory);
        final Account transferAccount = new Account(-1L, "transfer account");
        when(importFile.parse(inputStream)).thenReturn(Arrays.asList(
                ImmutableListMultimap.of(dateField, "01/15/1990", amountField, "10", categoryField, CATEGORY_CODE, sharesField, "2"),
                ImmutableListMultimap.of(dateField, "01/15/1990", amountField, "5", transferField, TRANSFER_ACCOUNT),
                ImmutableListMultimap.of(dateField, "01/15/1990", amountField, "1", categoryField, otherCategory, sharesField, "20")));
        ImportContext context = new GroupedDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);
        when(categoryMapper.get(CATEGORY_CODE)).thenReturn(category1);
        when(categoryMapper.get(otherCategory)).thenReturn(category2);
        when(importFile.getTransferAccount(TRANSFER_ACCOUNT)).thenReturn(transferAccount);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(3);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isSameAs(category1);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().toString()).isEqualTo("10");
        assertThat(transactions.get(0).getDetails().get(0).getAssetQuantity().toString()).isEqualTo("2");
        assertThat(transactions.get(0).getDetails().get(1).getTransferAccount()).isSameAs(transferAccount);
        assertThat(transactions.get(0).getDetails().get(1).getAmount().toString()).isEqualTo("5");
        assertThat(transactions.get(0).getDetails().get(2).getCategory()).isSameAs(category2);
        assertThat(transactions.get(0).getDetails().get(2).getAmount().toString()).isEqualTo("1");
        assertThat(transactions.get(0).getDetails().get(2).getAssetQuantity().toString()).isEqualTo("20");
    }

    @Test
    public void mergesDetailsForCategory() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TransactionCategory category = new TransactionCategory(CATEGORY_CODE);
        when(importFile.parse(inputStream)).thenReturn(Arrays.asList(
                ImmutableListMultimap.of(dateField, "01/15/1990", amountField, "10", categoryField, CATEGORY_CODE, sharesField, "2"),
                ImmutableListMultimap.of(dateField, "01/15/1990", amountField, "1", categoryField, CATEGORY_CODE, sharesField, "20")));
        ImportContext context = new GroupedDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);
        when(categoryMapper.get(CATEGORY_CODE)).thenReturn(category);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isSameAs(category);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().toString()).isEqualTo("11");
        assertThat(transactions.get(0).getDetails().get(0).getAssetQuantity().toString()).isEqualTo("22");
    }

    @Test
    public void mergesDetailsForTransfer() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final Account transferAccount = new Account(-1L, "transfer account");
        when(importFile.parse(inputStream)).thenReturn(Arrays.asList(
                ImmutableListMultimap.of(dateField, "01/15/1990", amountField, "10", transferField, TRANSFER_ACCOUNT),
                ImmutableListMultimap.of(dateField, "01/15/1990", amountField, "1", transferField, TRANSFER_ACCOUNT)));
        ImportContext context = new GroupedDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);
        when(importFile.getTransferAccount(TRANSFER_ACCOUNT)).thenReturn(transferAccount);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isNull();
        assertThat(transactions.get(0).getDetails().get(0).getTransferAccount()).isSameAs(transferAccount);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().toString()).isEqualTo("11");
    }
}