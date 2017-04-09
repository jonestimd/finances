package io.github.jonestimd.finance.file;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMultimap;
import io.github.jonestimd.finance.domain.account.Account;
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
    private DomainMapper<TransactionCategory> categoryMapper;
    @Mock
    private FieldValueExtractor fieldValueExtractor;

    private final ImportField dateField = new ImportFieldBuilder().type(FieldType.DATE).dateFormat("MM/dd/yyyy").get();
    private final ImportField payeeField = new ImportFieldBuilder().type(FieldType.PAYEE).get();
    private final ImportField amountField = new ImportFieldBuilder().type(FieldType.AMOUNT).amountFormat(AmountFormat.DECIMAL).get();
    private final ImportField categoryField = new ImportFieldBuilder().type(FieldType.CATEGORY).get();

    @Test
    public void populatesTransaction() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final Payee payee = new Payee();
        final Account account = new Account();
        when(importFile.getAccount()).thenReturn(account);
        when(importFile.getFieldValueExtractor()).thenReturn(fieldValueExtractor);
        SingleDetailImportContext context = new SingleDetailImportContext(importFile, payeeMapper, categoryMapper);
        when(fieldValueExtractor.parse(inputStream))
                .thenReturn(Collections.singletonList(ImmutableMultimap.of(dateField, "01/15/1990", payeeField, PAYEE_NAME)));
        when(payeeMapper.get(PAYEE_NAME)).thenReturn(payee);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getAccount()).isSameAs(account);
        assertThat(transactions.get(0).getDate()).isEqualTo(new SimpleDateFormat("yyyy/MM/dd").parse("1990/01/15"));
        assertThat(transactions.get(0).getPayee()).isSameAs(payee);
    }

    @Test
    public void populatesDetail() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TransactionCategory category = new TransactionCategory();
        when(importFile.getFieldValueExtractor()).thenReturn(fieldValueExtractor);
        SingleDetailImportContext context = new SingleDetailImportContext(importFile, payeeMapper, categoryMapper);
        when(fieldValueExtractor.parse(inputStream))
                .thenReturn(Collections.singletonList(ImmutableMultimap.of(amountField, "10", categoryField, CATEGORY_CODE)));
        when(categoryMapper.get(CATEGORY_CODE)).thenReturn(category);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().compareTo(BigDecimal.TEN)).isEqualTo(0);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isSameAs(category);
    }

    @Test
    public void populatesTransfer() throws Exception {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream("".getBytes());
        final TransactionCategory category = new TransactionCategory();
        final Account transferAccount = new Account();
        when(importFile.getFieldValueExtractor()).thenReturn(fieldValueExtractor);
        when(importFile.getTransferAccount(TRANSFER_ACCOUNT)).thenReturn(transferAccount);
        SingleDetailImportContext context = new SingleDetailImportContext(importFile, payeeMapper, categoryMapper);
        when(fieldValueExtractor.parse(inputStream))
                .thenReturn(Collections.singletonList(ImmutableMultimap.of(amountField, "10", categoryField, TRANSFER_ACCOUNT)));
        when(categoryMapper.get(CATEGORY_CODE)).thenReturn(category);

        List<Transaction> transactions = context.parseTransactions(inputStream);

        assertThat(transactions).hasSize(1);
        assertThat(transactions.get(0).getDetails()).hasSize(1);
        assertThat(transactions.get(0).getDetails().get(0).getAmount().compareTo(BigDecimal.TEN)).isEqualTo(0);
        assertThat(transactions.get(0).getDetails().get(0).getCategory()).isNull();
        assertThat(transactions.get(0).getDetails().get(0).getRelatedDetail().getTransaction().getAccount()).isSameAs(transferAccount);
    }
}