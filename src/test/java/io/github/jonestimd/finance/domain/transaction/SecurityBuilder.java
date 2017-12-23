package io.github.jonestimd.finance.domain.transaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import io.github.jonestimd.finance.domain.TestDomainUtils;
import io.github.jonestimd.finance.domain.asset.Security;
import io.github.jonestimd.finance.domain.asset.SplitRatio;

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

    public SecurityBuilder splits() {
        security.setSplits(new ArrayList<>());
        return this;
    }

    public SecurityBuilder splits(Date... splitDates) {
        security.setSplits(Arrays.stream(splitDates).map(this::newSplit).collect(Collectors.toList()));
        return this;
    }

    private StockSplit newSplit(Date date) {
        return TestDomainUtils.setId(new StockSplit(security, date, new SplitRatio(BigDecimal.ONE, BigDecimal.ONE)));
    }

    public SecurityBuilder splits(StockSplit ... splits) {
        security.setSplits(new ArrayList<>(Arrays.asList(splits)));
        return this;
    }

    public Security get() {
        return security;
    }
}
