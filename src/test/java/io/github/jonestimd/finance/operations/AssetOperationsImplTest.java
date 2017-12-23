package io.github.jonestimd.finance.operations;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.github.jonestimd.finance.dao.MockDaoContext;
import io.github.jonestimd.finance.dao.SecurityDao;
import io.github.jonestimd.finance.dao.StockSplitDao;
import io.github.jonestimd.finance.domain.account.Account;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SecuritySummary;
import io.github.jonestimd.finance.domain.asset.SecurityType;
import io.github.jonestimd.finance.domain.transaction.SecurityBuilder;
import io.github.jonestimd.finance.domain.transaction.StockSplit;
import io.github.jonestimd.finance.service.ServiceContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AssetOperationsImplTest {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private MockDaoContext daoRepository = new MockDaoContext();
    private SecurityDao securityDao;
    private StockSplitDao stockSplitDao;
    private AssetOperations assetOperations;

    @Before
    public void resetMocks() throws Exception {
        securityDao = daoRepository.getSecurityDao();
        stockSplitDao = daoRepository.getStockSplitDao();
        ServiceContext serviceContext = new ServiceContext(daoRepository);
        assetOperations = serviceContext.getAssetOperations();
        daoRepository.resetMocks();
    }

    @Test
    public void testGetAllSecurities() throws Exception {
        daoRepository.expectCommit();
        List<Security> securities = new ArrayList<>();
        when(securityDao.getAll()).thenReturn(securities);

        assertThat(assetOperations.getAllSecurities()).isSameAs(securities);
    }

    @Test
    public void getSecurityCallsDao() throws Exception {
        daoRepository.expectCommit();
        Security stock = new Security();
        String symbol = "xxx";
        when(securityDao.getSecurity(symbol)).thenReturn(stock);

        assertThat(assetOperations.getSecurity(symbol)).isSameAs(stock);
    }

    @Test
    public void saveCallsDao() throws Exception {
        daoRepository.expectCommit();
        Security stock = new Security();
        when(securityDao.save(stock)).thenReturn(stock);

        assertThat(assetOperations.save(stock)).isSameAs(stock);
    }

    @Test
    public void createIfUniqueCallsDaoSave() throws Exception {
        daoRepository.expectCommit();
        Security stock = new Security();
        when(securityDao.getSecurity(null)).thenReturn(null);
        when(securityDao.save(stock)).thenReturn(stock);

        assertThat(assetOperations.createIfUnique(stock)).isSameAs(stock);
    }

    @Test
    public void createIfUniqueDoesntSaveDuplicate() throws Exception {
        daoRepository.expectCommit();
        String symbol = "xxx";
        String type = "type";
        Security existingStock = new Security();
        existingStock.setType(type);
        Security stock = new Security();
        stock.setType(type);
        stock.setSymbol(symbol);
        when(securityDao.getSecurity(symbol)).thenReturn(existingStock);

        assertThat(assetOperations.createIfUnique(stock)).isSameAs(existingStock);
    }

    @Test
    public void createIfUniqueThrowsExceptionIfTypesDontMatch() throws Exception {
        daoRepository.expectRollback();
        String symbol = "xxx";
        Security existingStock = new Security();
        existingStock.setType("other");
        Security stock = new Security();
        stock.setType("type");
        stock.setSymbol(symbol);
        when(securityDao.getSecurity(symbol)).thenReturn(existingStock);

        try {
            assetOperations.createIfUnique(stock);
            fail("expected an exception");
        }
        catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage()).isEqualTo("Imported type for security xxx doesn't match the existing value.");
        }
    }

    @Test
    public void findOrCreateReturnsExistingSecurity() throws Exception {
        daoRepository.expectCommit();
        Security security = new Security();
        String securityName = "Security Name";
        when(securityDao.findByName(securityName)).thenReturn(security);

        assertThat(assetOperations.findOrCreate(securityName)).isSameAs(security);
    }

    @Test
    public void findOrCreateReturnsNewSecurityForNoMatch() throws Exception {
        daoRepository.expectCommit();
        String securityName = "Security Name";
        when(securityDao.findByName(securityName)).thenReturn(null);

        Security result = assetOperations.findOrCreate(securityName);

        ArgumentCaptor<Security> capture = ArgumentCaptor.forClass(Security.class);
        verify(securityDao).save(capture.capture());
        assertThat(capture.getValue()).isSameAs(result);
        assertThat(capture.getValue().getName()).isSameAs(securityName);
        assertThat(capture.getValue().getType()).isSameAs(SecurityType.STOCK.getValue());
    }

    @Test
    public void addSplitToExistingSecurity() throws Exception {
        daoRepository.expectCommit();
        String securityName = "Security Name";
        Date date = new Date();
        Security security = new Security();
        when(securityDao.findByName(securityName)).thenReturn(security);
        when(stockSplitDao.find(security, date)).thenReturn(null);

        StockSplit split = assetOperations.findOrCreateSplit(securityName, date, BigDecimal.ONE, new BigDecimal(1.5d));

        ArgumentCaptor<StockSplit> capture = ArgumentCaptor.forClass(StockSplit.class);
        verify(stockSplitDao).save(capture.capture());
        assertThat(capture.getValue()).isSameAs(split);
        assertThat(split.getSecurity()).isSameAs(security);
        assertThat(split.getDate()).isSameAs(date);
        assertThat(split.getSplitRatio().getSharesIn()).isEqualTo(BigDecimal.ONE);
        assertThat(split.getSplitRatio().getSharesOut().doubleValue()).isEqualTo(1.5d);
    }

    @Test
    public void saveSplitsReplacesSplits() throws Exception {
        daoRepository.expectCommit();
        Security persisted = new SecurityBuilder().get();
        persisted.setSplits(new ArrayList<>());
        Security updated = new SecurityBuilder().nextId().splits(parseDate("2000-01-01"), parseDate("2005-01-01")).get();
        when(securityDao.get(updated.getId())).thenReturn(persisted);
        List<SecuritySummary> result = singletonList(new SecuritySummary());
        when(securityDao.getSecuritySummaryByAccount(updated.getId())).thenReturn(result);

        assertThat(assetOperations.saveSplits(updated)).isSameAs(result);

        assertThat(persisted.getSplits()).containsExactlyElementsOf(updated.getSplits());
    }

    private Date parseDate(String source) throws ParseException {
        return dateFormat.parse(source);
    }

    @Test
    public void addDuplicateSplitToExistingSecurity() throws Exception {
        StockSplit split = new StockSplit();
        daoRepository.expectCommit();
        String securityName = "Security Name";
        Date date = new Date();
        Security security = new Security();
        when(securityDao.findByName(securityName)).thenReturn(security);
        when(stockSplitDao.find(security, date)).thenReturn(split);

        assertThat(assetOperations.findOrCreateSplit(securityName, date, BigDecimal.ONE, new BigDecimal(1.5d))).isSameAs(split);
    }

    @Test
    public void getSecuritySummariesForAccount() throws Exception {
        daoRepository.expectCommit();
        final Account account = new Account(-1L);
        final SecuritySummary securitySummary = new SecuritySummary(new Security(), 0L, BigDecimal.ONE, new Account(1L));
        List<SecuritySummary> expected = singletonList(securitySummary);
        when(securityDao.getSecuritySummaries(account.getId())).thenReturn(expected);

        List<SecuritySummary> result = assetOperations.getSecuritySummaries(account);

        assertThat(result).isSameAs(expected);
    }

    @Test
    public void getSecuritySummariesByAccount() throws Exception {
        daoRepository.expectCommit();
        final SecuritySummary securitySummary = new SecuritySummary(new Security(), 0L, BigDecimal.ONE, new Account(1L));
        List<SecuritySummary> expected = singletonList(securitySummary);
        when(securityDao.getSecuritySummariesByAccount()).thenReturn(expected);

        List<SecuritySummary> result = assetOperations.getSecuritySummariesByAccount();

        assertThat(result).isSameAs(expected);
    }

    @Test
    public void deleteAllCallsDao() throws Exception {
        List<Security> securities = new ArrayList<>();

        assetOperations.deleteAll(securities);

        verify(securityDao).deleteAll(same(securities));
    }
}