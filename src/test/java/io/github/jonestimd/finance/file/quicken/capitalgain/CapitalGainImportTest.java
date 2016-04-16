package io.github.jonestimd.finance.file.quicken.capitalgain;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Currency;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SplitRatio;
import io.github.jonestimd.finance.domain.transaction.SecurityLot;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import io.github.jonestimd.finance.domain.transaction.Transaction;
import io.github.jonestimd.finance.domain.transaction.TransactionDetail;
import io.github.jonestimd.finance.file.quicken.QuickenException;
import io.github.jonestimd.finance.service.TransactionService;
import io.github.jonestimd.finance.swing.securitylot.LotAllocationDialog;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CapitalGainImportTest {
    private static final String COLUMN_NAMES = "Acct\tSecurity\tShares\tBought\tSold\tSales Price\tCost Basis\tGain/Loss\n";
    private TransactionService transactionService = mock(TransactionService.class);
    private LotAllocationDialog lotAllocationDialog = mock(LotAllocationDialog.class);
    private Account account = new Account();
    private ListMultimap<Date, TransactionDetail> purchaseMap = ArrayListMultimap.create();
    private Answer<List<TransactionDetail>> purchasesAnswer = new Answer<List<TransactionDetail>>() {
        public List<TransactionDetail> answer(InvocationOnMock invocation) throws Throwable {
            return purchaseMap.get((Date) invocation.getArguments()[2]);
        }
    };
    @Captor
    private ArgumentCaptor<Iterable<SecurityLot>> lotCapture;

    public CapitalGainImportTest() {
        account.setCurrency(new Currency(java.util.Currency.getInstance("USD")));
        Logger.getLogger(CapitalGainImport.class).setLevel(Level.DEBUG);
    }

    private Reader createReader(String input) {
        return new CharArrayReader(input.toCharArray());
    }

    private Date createDate(String mmddyyyy) throws ParseException {
        return new SimpleDateFormat("MM/dd/yyyy").parse(mmddyyyy);
    }

    @Test
    public void versionRecordIgnored() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);

        txfImport.importFile(createReader(COLUMN_NAMES));
    }

    @Test
    public void multipleLotsPerSale() throws Exception {
        Security security1 = createSecurity("SECURITY 123");
        Security security2 = createSecurity("SECURITY ABC");
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        TransactionDetail sale1 = createTransaction("02/28/2005", security1, "200.00", "-20");
        TransactionDetail purchase1 = createPurchase("01/20/2000", security1, "-150.00", "30");
        TransactionDetail purchase2 = createPurchase("01/20/2001", security1, "-50.00", "5");
        TransactionDetail sale2 = createTransaction("02/28/1999", security2, "33.33", "-10");
        TransactionDetail purchase3 = createPurchase("01/20/1991", security2, "-44.44", "40");
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(Lists.newArrayList(sale1));
        when(transactionService.findSecuritySalesWithoutLots("Security A", createDate("02/28/1999")))
            .thenReturn(Lists.newArrayList(sale2));
        expectFindPurchases(security1);
        expectFindPurchases(security2);

        txfImport.importFile(createReader(COLUMN_NAMES +
            "X\tSecurity 1\t15.000\t01/20/00\t02/28/05\t150.00\t75.00\t0.00\n" +
        	"X\tSecurity 1\t22.222\t01/20/00\t02/28/05\t234.56\t123.45\t0.00\n" +
        	"X\tSecurity A\t10.000\t01/20/91\t02/28/99\t33.33\t11.10\t0.00\n" +
        	"X\tSecurity 1\t5.000\t01/20/01\t02/28/05\t50.00\t50.00\t0.00\n"));

        verify(transactionService).findSecuritySalesWithoutLots(eq("Security 1"), any(Date.class));
        verify(transactionService).findSecuritySalesWithoutLots(eq("Security A"), any(Date.class));
        verify(transactionService, times(2)).findPurchasesWithRemainingLots(same(account), same(security1), any(Date.class));
        verify(transactionService, times(1)).findPurchasesWithRemainingLots(same(account), same(security2), any(Date.class));
        verify(transactionService).saveSecurityLots(lotCapture.capture());
        assertEquals(3, Iterables.size(lotCapture.getValue()));
        checkLot(sale1, "15", purchase1, "15", lotCapture.getValue());
        checkLot(sale1, "5", purchase2, "5", lotCapture.getValue());
        checkLot(sale2, "10", purchase3, "10", lotCapture.getValue());
    }

    private void checkLot(TransactionDetail sale, String saleShares, TransactionDetail purchase, String purchasShares, Iterable<SecurityLot> lots) {
        for (SecurityLot lot: lots) {
            if (lot.getSale() == sale && lot.getSaleShares().compareTo(new BigDecimal(saleShares)) == 0 &&
                    lot.getPurchase() == purchase && lot.getPurchaseShares().compareTo(new BigDecimal(purchasShares)) == 0) {
                return;
            }
        }
        fail("lot not found: " + sale.getTransaction().getSecurity().getName() + " - " + saleShares + " - " + purchasShares);
    }

    @Test
    public void multipleSalesPerDay_differentSaleAmountsDifferentShares() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        Security security = createSecurity("SECURITY 123");
        TransactionDetail sale1 = createTransaction("02/28/2005", security, "200.00", "-20");
        TransactionDetail sale2 = createTransaction("02/28/2005", security, "180.00", "-15");
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(Lists.newArrayList(sale1, sale2));
        TransactionDetail purchase1 = createPurchase("01/20/2000", security, "-75.00", "15");
        TransactionDetail purchase2 = createPurchase("01/20/1991", security, "-11.11", "9");
        TransactionDetail purchase3 = createPurchase("01/20/1992", security, "-11.11", "5");
        TransactionDetail purchase4 = createPurchase("01/20/2001", security, "-50.00", "6");
        expectFindPurchases(security);

        txfImport.importFile(createReader(COLUMN_NAMES +
                "X\tSecurity 1\t15.000\t01/20/00\t02/28/05\t150.00\t75.00\t0.00\n" +
                "X\tSecurity 1\t9.000\t01/20/91\t02/28/05\t108.00\t11.11\t0.00\n" +
                "X\tSecurity 1\t5.000\t01/20/92\t02/28/05\t50.00\t11.11\t0.00\n" +
                "X\tSecurity 1\t6.000\t01/20/01\t02/28/05\t72.00\t50.00\t0.00\n"));

        verify(transactionService, times(4)).findPurchasesWithRemainingLots(same(account), same(security), any(Date.class));
        verify(transactionService).saveSecurityLots(lotCapture.capture());
        assertEquals(4, Iterables.size(lotCapture.getValue()));
        checkLot(sale1, "15", purchase1, "15", lotCapture.getValue());
        checkLot(sale1, "5", purchase3, "5", lotCapture.getValue());
        checkLot(sale2, "9", purchase2, "9", lotCapture.getValue());
        checkLot(sale2, "6", purchase4, "6", lotCapture.getValue());
    }

    @Test
    public void multipleSalesPerDay_sameSaleAmountsDifferentShares() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        Security security = createSecurity("SECURITY 123");
        TransactionDetail sale1 = createTransaction("02/28/2005", security, "220.00", "-22.000001");
        TransactionDetail sale2 = createTransaction("02/28/2005", security, "220.00", "-10.999999");
        TransactionDetail purchase1 = createPurchase("01/20/2000", security, "-75.00", "16.000001");
        TransactionDetail purchase2 = createPurchase("01/20/1991", security, "-22.22", "7.999999");
        TransactionDetail purchase3 = createPurchase("01/20/1991", security, "-11.11", "3");
        TransactionDetail purchase4 = createPurchase("01/20/2001", security, "-50.00", "6");
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(Lists.newArrayList(sale1, sale2));
        expectFindPurchases(security);

        txfImport.importFile(createReader(COLUMN_NAMES +
                "X\tSecurity 1\t16.000\t01/20/00\t02/28/05\t160.00\t75.00\t0.00\n" +
                "X\tSecurity 1\t8.000\t01/20/91\t02/28/05\t160.00\t22.22\t0.00\n" +
                "X\tSecurity 1\t3.000\t01/20/91\t02/28/05\t60.00\t11.11\t0.00\n" +
                "X\tSecurity 1\t6.000\t01/20/01\t02/28/05\t60.00\t50.00\t0.00\n"));

        verify(transactionService, times(3)).findPurchasesWithRemainingLots(same(account), same(security), any(Date.class));
        verify(transactionService).saveSecurityLots(lotCapture.capture());
        assertEquals(4, Iterables.size(lotCapture.getValue()));
        checkLot(sale1, "16.000001", purchase1, "16.000001", lotCapture.getValue());
        checkLot(sale1, "6", purchase4, "6", lotCapture.getValue());
        checkLot(sale2, "7.999999", purchase2, "7.999999", lotCapture.getValue());
        checkLot(sale2, "3", purchase3, "3", lotCapture.getValue());
    }

    @Test
    public void matchPurchasePrice() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        Security security = createSecurity("SECURITY 123");
        TransactionDetail sale1 = createTransaction("02/28/2005", security, "320.00", "-32.0");
        TransactionDetail purchase1 = createPurchase("01/20/2000", security, "-80.00", "16.0");
        createPurchase("01/20/2000", security, "-192.00", "32.0");
        TransactionDetail purchase3 = createPurchase("01/20/1991", security, "-120.00", "24.0");
        createPurchase("01/20/1991", security, "-288.00", "48.0");
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(Arrays.asList(sale1));
        expectFindPurchases(security);

        txfImport.importFile(createReader(COLUMN_NAMES +
                "X\tSecurity 1\t16.000\t01/20/00\t02/28/05\t160.00\t80.00\t0.00\n" +
                "X\tSecurity 1\t16.000\t01/20/91\t02/28/05\t160.00\t80.00\t0.00\n"));

        verify(transactionService, times(2)).findPurchasesWithRemainingLots(same(account), same(security), any(Date.class));
        verify(transactionService).saveSecurityLots(lotCapture.capture());
        assertEquals(2, Iterables.size(lotCapture.getValue()));
        checkLot(sale1, "16", purchase1, "16", lotCapture.getValue());
        checkLot(sale1, "16", purchase3, "16", lotCapture.getValue());
    }

    @Test
    public void multiplePurchasesPerDay_samePrice() throws Exception { // 40 (25+15) 30 (20+10)
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        Security security = createSecurity("SECURITY abc123");
        TransactionDetail sale1 = createTransaction("02/28/2005", security, "25.00", "-25");
        TransactionDetail sale2 = createTransaction("02/28/2005", security, "10.00", "-10");
        TransactionDetail sale3 = createTransaction("02/28/2005", security, "35.00", "-35");
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(Lists.newArrayList(sale1, sale2, sale3));
        TransactionDetail purchase1 = createPurchase("01/20/1991", security, "-40.00", "40");
        TransactionDetail purchase2 = createPurchase("01/20/1991", security, "-30.00", "30");
        expectFindPurchases(security);

        txfImport.importFile(createReader(COLUMN_NAMES +
                "X\tSecurity 1\t25.000\t01/20/91\t02/28/05\t25.00\t25.00\t0.00\n" +
                "X\tSecurity 1\t20.000\t01/20/91\t02/28/05\t20.00\t20.00\t0.00\n" +
                "X\tSecurity 1\t10.000\t01/20/91\t02/28/05\t10.00\t10.00\t0.00\n" +
                "X\tSecurity 1\t15.000\t01/20/91\t02/28/05\t15.00\t15.00\t0.00\n"));

        verify(transactionService, times(1)).findPurchasesWithRemainingLots(same(account), same(security), any(Date.class));
        verify(transactionService).saveSecurityLots(lotCapture.capture());
        assertEquals(4, Iterables.size(lotCapture.getValue()));
        checkLot(sale1, "25", purchase1, "25", lotCapture.getValue());
        checkLot(sale2, "10", purchase2, "10", lotCapture.getValue());
        checkLot(sale3, "15", purchase1, "15", lotCapture.getValue());
        checkLot(sale3, "20", purchase2, "20", lotCapture.getValue());
    }

    @Test
    public void zeroAmountsInImport() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        Security security = createSecurity("SECURITY 123");
        TransactionDetail sale1 = createTransaction("02/28/2005", security, "0", "-32.0");
        TransactionDetail purchase1 = createTransaction("01/20/2000", security, "0.00", "20.0");
        TransactionDetail purchase2 = createTransaction("01/20/1991", security, "0.00", "20.0");
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(Lists.newArrayList(sale1));
        when(transactionService.findPurchasesWithRemainingLots(account, security, createDate("01/20/1991")))
            .thenReturn(Lists.newArrayList(purchase2));
        when(transactionService.findPurchasesWithRemainingLots(account, security, createDate("01/20/2000")))
            .thenReturn(Lists.newArrayList(purchase1));

        txfImport.importFile(createReader(COLUMN_NAMES +
                "X\tSecurity 1\t16.000\t01/20/00\t02/28/05\t0.00\t0.00\t0.00\n" +
                "X\tSecurity 1\t16.000\t01/20/91\t02/28/05\t0.00\t0.00\t0.00\n"));

        verify(transactionService).saveSecurityLots(lotCapture.capture());
        assertEquals(2, Iterables.size(lotCapture.getValue()));
        checkLot(sale1, "16", purchase1, "16", lotCapture.getValue());
        checkLot(sale1, "16", purchase2, "16", lotCapture.getValue());
    }

    @Test
    public void zeroAmountInSharesOut() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        Security security = createSecurity("SECURITY 123");
        TransactionDetail sale1 = createTransaction("02/28/2005", security, "0", "-31.0");
        TransactionDetail sale2 = createTransaction("02/28/2005", security, "465", "-31.0");
        TransactionDetail purchase1 = createTransaction("01/20/1991", security, "0.00", "16.0");
        TransactionDetail purchase2 = createTransaction("01/20/1991", security, "0.00", "15.0");
        TransactionDetail purchase3 = createTransaction("01/20/2000", security, "0.00", "16.0");
        TransactionDetail purchase4 = createTransaction("01/20/2000", security, "0.00", "15.0");
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(Lists.newArrayList(sale1, sale2));
        when(transactionService.findPurchasesWithRemainingLots(account, security, createDate("01/20/1991")))
            .thenReturn(Lists.newArrayList(purchase2, purchase1));
        when(transactionService.findPurchasesWithRemainingLots(account, security, createDate("01/20/2000")))
            .thenReturn(Lists.newArrayList(purchase3, purchase4));

        txfImport.importFile(createReader(COLUMN_NAMES +
                "X\tSecurity 1\t16.000\t01/20/00\t02/28/05\t160.00\t160.00\t0.00\n" +
                "X\tSecurity 1\t15.000\t01/20/91\t02/28/05\t225.00\t75.00\t0.00\n" +
                "X\tSecurity 1\t16.000\t01/20/91\t02/28/05\t240.00\t80.00\t0.00\n" +
                "X\tSecurity 1\t15.000\t01/20/00\t02/28/05\t150.00\t150.00\t0.00\n"));

        verify(transactionService).saveSecurityLots(lotCapture.capture());
        assertEquals(4, Iterables.size(lotCapture.getValue()));
        checkLot(sale2, "15", purchase2, "15", lotCapture.getValue());
        checkLot(sale2, "16", purchase1, "16", lotCapture.getValue());
        checkLot(sale1, "16", purchase3, "16", lotCapture.getValue());
        checkLot(sale1, "15", purchase4, "15", lotCapture.getValue());
    }

    @Test
    public void salesWithSplits() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        Security security = createSecurity("SECURITY 123");
        security.setSplits(Arrays.asList(
                createSplit(createDate("06/01/1995"), new BigDecimal(2)),
                createSplit(createDate("06/01/2002"), new BigDecimal(2))));
        TransactionDetail sale1 = createTransaction("02/28/2005", security, "320.00", "-32.0");
        TransactionDetail purchase1 = createTransaction("01/20/2000", security, "-40.00", "8.0");
        TransactionDetail purchase2 = createTransaction("01/20/1991", security, "-30.00", "6.0");
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(Lists.newArrayList(sale1));
        when(transactionService.findPurchasesWithRemainingLots(account, security, createDate("01/20/1991")))
            .thenReturn(Lists.newArrayList(purchase2));
        when(transactionService.findPurchasesWithRemainingLots(account, security, createDate("01/20/2000")))
            .thenReturn(Lists.newArrayList(purchase1));

        txfImport.importFile(createReader(COLUMN_NAMES +
                "X\tSecurity 1\t16.000\t01/20/00\t02/28/05\t160.00\t40.00\t0.00\n" +
                "X\tSecurity 1\t16.000\t01/20/91\t02/28/05\t160.00\t20.00\t0.00\n"));

        verify(transactionService).saveSecurityLots(lotCapture.capture());
        assertEquals(2, Iterables.size(lotCapture.getValue()));
        checkLot(sale1, "16", purchase1, "8", lotCapture.getValue());
        checkLot(sale1, "16", purchase2, "4", lotCapture.getValue());
    }

    @Test
    public void noMatchingSale() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(new ArrayList<TransactionDetail>());

        txfImport.importFile(createReader(COLUMN_NAMES +
                "X\tSecurity 1\t16.000\t01/20/00\t02/28/05\t160.00\t80.00\t0.00\n" +
                "X\tSecurity 1\t16.000\t01/20/91\t02/28/05\t160.00\t80.00\t0.00\n"));
    }

    @Test
    public void noMatchingPurchases() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);
        Security security = createSecurity("SECURITY 123");
        TransactionDetail sale = createTransaction("02/28/2005", security, "320.00", "-32.0");
        TransactionDetail purchase1 = createTransaction("01/20/2000", security, "-80.00", "16.0");
        when(transactionService.findSecuritySalesWithoutLots("Security 1", createDate("02/28/2005")))
            .thenReturn(Lists.newArrayList(sale));
        when(transactionService.findPurchasesWithRemainingLots(account, security, createDate("01/20/1991")))
            .thenReturn(new ArrayList<TransactionDetail>());
        when(transactionService.findPurchasesWithRemainingLots(account, security, createDate("01/20/2000")))
            .thenReturn(Lists.newArrayList(purchase1));

        txfImport.importFile(createReader(COLUMN_NAMES +
                "X\tSecurity 1\t16.000\t01/20/00\t02/28/05\t160.00\t80.00\t0.00\n" +
                "X\tSecurity 1\t16.000\t01/20/91\t02/28/05\t160.00\t80.00\t0.00\n"));
    }

    private void expectFindPurchases(Security security) {
        when(transactionService.findPurchasesWithRemainingLots(same(account), same(security), any(Date.class)))
            .thenAnswer(purchasesAnswer);
    }

    private TransactionDetail createPurchase(String date, Security security, String detailAmount, String shares) throws Exception {
        TransactionDetail purchase = createTransaction(date, security, detailAmount, shares);
        purchaseMap.put(purchase.getTransaction().getDate(), purchase);
        return purchase;
    }

    private TransactionDetail createTransaction(String date, Security security, String detailAmount, String shares) throws Exception {
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setDate(createDate(date));
        transaction.addDetails(createDetail(detailAmount, shares));
        transaction.setSecurity(security);
        return transaction.getDetails().get(0);
    }

    private TransactionDetail createDetail(String detailAmount, String shares) {
        TransactionDetail detail = new TransactionDetail(null, new BigDecimal(detailAmount), null, null);
        detail.setAssetQuantity(new BigDecimal(shares));
        return detail;
    }

    private Security createSecurity(String name) {
        Security security = new Security();
        security.setName(name);
        return security;
    }

    private StockSplit createSplit(Date date, BigDecimal ratio) {
        StockSplit split = new StockSplit();
        split.setDate(date);
        split.setSplitRatio(new SplitRatio(BigDecimal.ONE, ratio));
        return split;
    }

    @Test
    public void invalidRecordException() throws Exception {
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);

        try {
            txfImport.importFile(createReader(COLUMN_NAMES +
                    "\t\n" +
            		"\t\n" +
            		"Account x\t\t\n"));
            fail("expected exception");
        }
        catch (QuickenException ex) {
            assertEquals("io.github.jonestimd.finance.file.quicken.invalidRecord", ex.getMessageKey());
            assertArrayEquals(new Object[] {"TSV", 4L}, ex.getMessageArgs());
        }
    }

    @Test
    public void readerException() throws Exception {
        Reader reader = mock(Reader.class);
        when(reader.read(any(char[].class), anyInt(), anyInt())).thenThrow(new IOException());
        CapitalGainImport txfImport = new CapitalGainImport(transactionService, lotAllocationDialog);

        try {
            txfImport.importFile(reader);
            fail("expected exception");
        }
        catch (QuickenException ex) {
            assertEquals("io.github.jonestimd.finance.file.quicken.importFailed", ex.getMessageKey());
            assertArrayEquals(new Object[] {"TXF", 0L}, ex.getMessageArgs());
        }
    }
}