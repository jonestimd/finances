package io.github.jonestimd.finance.file;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
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
import org.mockito.runners.MockitoJUnitRunner;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SingleDetailImportContextTest {
    public static final String CATEGORY_CODE = "category code";
    public static final String PAYEE_NAME = "Payee name";
    public static final String TRANSFER_ACCOUNT = "Transfer account";
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
    private final ImportField amountField = new ImportFieldBuilder().type(FieldType.AMOUNT).amountFormat(AmountFormat.DECIMAL).get();
    private final ImportField sharesField = new ImportFieldBuilder().type(FieldType.ASSET_QUANTITY).amountFormat(AmountFormat.DECIMAL).get();
    private final ImportField categoryField = new ImportFieldBuilder().type(FieldType.CATEGORY).get();

    @Test
    public void populatesTransaction() throws Exception {
        final Date date = new Date();
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final Payee payee = new Payee();
        final Account account = new Account();
        when(importFile.parseDate(anyString())).thenReturn(date);
        when(importFile.getAccount()).thenReturn(account);
        when(importFile.parse(inputStream)).thenReturn(singletonList(ImmutableListMultimap.of(dateField, "01/15/1990", payeeField, PAYEE_NAME)));
        ImportContext context = new SingleDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);
        when(payeeMapper.get(PAYEE_NAME)).thenReturn(payee);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAccount()).isSameAs(account);
        assertThat(transactions.get(0).getDate()).isSameAs(date);
        assertThat(transactions.get(0).getPayee()).isSameAs(payee);
        verify(importFile).parseDate("01/15/1990");
    }

    @Test
    public void populatesDetail() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TransactionCategory category = new TransactionCategory();
        when(importFile.parse(inputStream)).thenReturn(singletonList(
                ImmutableListMultimap.of(amountField, "10", categoryField, CATEGORY_CODE, sharesField, "5")));
        ImportContext context = new SingleDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);
        when(categoryMapper.get(CATEGORY_CODE)).thenReturn(category);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isSameAs(category);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().toString()).isEqualTo("10");
        assertThat(transactions.get(0).getDetails().get(0).getAssetQuantity().toString()).isEqualTo("-5");
    }

    @Test
    public void negatesAmountBasedOnCategory() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TransactionCategory category = new TransactionCategory();
        when(importFile.parse(inputStream)).thenReturn(singletonList(
                ImmutableListMultimap.of(amountField, "10", categoryField, CATEGORY_CODE, sharesField, "5")));
        ImportContext context = new SingleDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);
        when(categoryMapper.get(CATEGORY_CODE)).thenReturn(category);
        when(importFile.isNegate(category)).thenReturn(true);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isSameAs(category);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().toString()).isEqualTo("-10");
        assertThat(transactions.get(0).getDetails().get(0).getAssetQuantity().toString()).isEqualTo("5");
    }

    @Test
    public void excludesZeroAssetQuantity() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TransactionCategory category = new TransactionCategory();
        when(importFile.parse(inputStream)).thenReturn(singletonList(ImmutableListMultimap.of(amountField, "10", categoryField, CATEGORY_CODE, sharesField, "0.0")));
        ImportContext context = new SingleDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);
        when(categoryMapper.get(CATEGORY_CODE)).thenReturn(category);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isSameAs(category);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().compareTo(BigDecimal.TEN)).isEqualTo(0);
        assertThat(transactions.get(0).getDetails().get(0).getAssetQuantity()).isNull();
    }

    @Test
    public void populatesTransfer() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TransactionCategory category = new TransactionCategory();
        final Account transferAccount = new Account();
        when(importFile.parse(inputStream)).thenReturn(singletonList(ImmutableListMultimap.of(amountField, "10", categoryField, TRANSFER_ACCOUNT)));
        when(importFile.getTransferAccount(TRANSFER_ACCOUNT)).thenReturn(transferAccount);
        SingleDetailImportContext context = new SingleDetailImportContext(importFile, payeeMapper, securityMapper, categoryMapper);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().compareTo(BigDecimal.TEN)).isEqualTo(0);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isNull();
        assertThat(transactions.get(0).getDetails().get(0).getRelatedDetail().getTransaction().getAccount()).isSameAs(transferAccount);
    }
}