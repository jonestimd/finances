package io.github.jonestimd.finance.domain.transaction;

import java.util.Arrays;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.asset.Security;

public class SecurityBuilder {
    private final Security security = new Security();

    public SecurityBuilder() {}

    public SecurityBuilder(Security security) {
        TestDomainUtils.setId(this.security, security.getId());
        this.security.setName(security.getName());
        this.security.setType(security.getType());
        this.security.setSymbol(security.getSymbol());
    }

    public SecurityBuilder nextId() {
        TestDomainUtils.setId(security);
        return this;
    }

    public SecurityBuilder name(String name) {
        security.setName(name);
        return this;
    }

    public SecurityBuilder symbol(String symbol) {
        security.setSymbol(symbol);
        return this;
    }

    public SecurityBuilder splits(StockSplit ... splits) {
        security.setSplits(Arrays.asList(splits));
        return this;
    }

    public Security get() {
        return security;
    }
}
