package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;

import io.github.jonestimd.finance.domain.TestDomainUtils;

public class SecurityLotBuilder {
    private SecurityLot lot = new SecurityLot();

    public SecurityLotBuilder nextId() throws Exception {
        TestDomainUtils.setId(lot);
        return this;
    }

    public SecurityLotBuilder purchase(TransactionDetail purchase, BigDecimal shares) {
        lot.setPurchase(purchase);
        lot.setAdjustedShares(shares);
        if (purchase != null) purchase.getSaleLots().add(lot);
        return this;
    }
    
    public SecurityLotBuilder sale(TransactionDetail sale, BigDecimal shares) {
        lot.setSale(sale);
        lot.setAdjustedShares(shares);
        if (sale != null) sale.getPurchaseLots().add(lot);
        return this;
    }

    public SecurityLot get() {
        return lot;
    }
}
