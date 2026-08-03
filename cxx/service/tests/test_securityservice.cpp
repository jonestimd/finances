#include <QTest>
#include "dbtestcase.h"
#include "service/securityservice.h"
#include "service/servicecontext.h"

class TestSecurityService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

    QString normalize(QDecNumber value) {
        return value.normalize().toString();
    }

    void verifySecurity(const Security *security, int txCount, const char *shares, const char *costBasis) {
        QCOMPARE(security->transactions, txCount);
        QCOMPARE(normalize(security->shares), shares);
        QCOMPARE(normalize(security->costBasis), costBasis);
    }

    void verifyAccountSecurity(const AccountSecurity* summary, domain_id securityId, QDecNumber shares, QDecNumber costBasis, int transactions) {
        QCOMPARE(summary->id.securityId, securityId);
        QCOMPARE(summary->shares, shares);
        QCOMPARE(summary->costBasis, costBasis);
        QCOMPARE(summary->transactions, transactions);
    }

    domain_id addSplit(const QString &driver, domain_id securityId, QDate date, int sharesIn, int sharesOut) {
        Connection conn(dbTestCase.connectionPool(driver));
        StockSplit split{};
        split.securityId = securityId;
        split.date = date;
        split.sharesIn = sharesIn;
        split.sharesOut = sharesOut;
        dbTestCase.stockSplitDao(driver).add(conn.db, {&split}, TEST_USER);
        return split.id.value();
    }

    QList<const AccountSecurity*> forAccount(const QList<const AccountSecurity*> &summaries, domain_id accountId) {
        QList<const AccountSecurity*> matches;
        for (auto summary : summaries) if (summary->id.accountId == accountId) matches.append(summary);
        std::stable_sort(matches.begin(), matches.end(), [](const AccountSecurity* as1, const AccountSecurity* as2) {
            return as1->id.securityId < as2->id.securityId;
        });
        return matches;
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<SecurityService*>("service");
        QTest::addColumn<StockSplitService*>("splitService");
        QTest::addColumn<domain_id>("accountId");
        QTest::addColumn<domain_id>("accountId2");
        QTest::addColumn<domain_id>("securityId");
        QTest::addColumn<domain_id>("securityId2");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto &dao = dbTestCase.securityDao(driver);
            auto accountId = dbTestCase.addAccount(driver, "account 1", &AccountType::bank)->id.value();
            auto accountId2 = dbTestCase.addAccount(driver, "account 2", &AccountType::bank)->id.value();
            auto securityId = dbTestCase.addSecurity(driver, "security 1")->id.value();
            auto securityId2 = dbTestCase.addSecurity(driver, "security 2")->id.value();
            auto service = new SecurityService{dbTestCase.connectionPool(driver), dao};
            auto splitService = new StockSplitService{dbTestCase.connectionPool(driver), dbTestCase.stockSplitDao(driver)};
            QTest::newRow(driver.toLocal8Bit()) << driver << service << splitService << accountId << accountId2 << securityId << securityId2;
        }
    }

    void getAll_returnsTransactionSummary() {
        QFETCH_GLOBAL(QString, driver);
        dbTestCase.resetDatabase(driver);
        QFETCH_GLOBAL(SecurityService*, service);
        QFETCH_GLOBAL(domain_id, accountId);
        QFETCH_GLOBAL(domain_id, accountId2);
        QFETCH_GLOBAL(domain_id, securityId);
        QFETCH_GLOBAL(domain_id, securityId2);
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId), {"-1.23", "-2.00"}, {"3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId), {"-10.45"}, {"6"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, {}, securityId2), {"-2.00"}, {"5"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, {}, securityId2), {"20.00"}, {"-1"});

        auto result = service->getAll();

        verifySecurity(result.value(securityId), 2, "9", "11.68");
        verifySecurity(result.value(securityId2), 2, "4", "2");
    }

    void getAll_adjustsSharesForSplits() {
        QFETCH_GLOBAL(QString, driver);
        dbTestCase.resetDatabase(driver);
        QFETCH_GLOBAL(SecurityService*, service);
        QFETCH_GLOBAL(domain_id, accountId);
        QFETCH_GLOBAL(domain_id, securityId);
        QDate tx1Date{2010, 2, 15};
        QDate tx2Date = tx1Date.addDays(1);
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId, tx1Date), {"-1.00"}, {"3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId, tx2Date), {"-1.00"}, {"5"});
        addSplit(driver, securityId, tx1Date, 1, 2);

        auto result = service->getAll();

        verifySecurity(result.value(securityId), 2, "11", "2");
    }

    void getSplits_returnsAllSplits() {
        QFETCH_GLOBAL(QString, driver);
        dbTestCase.resetDatabase(driver);
        QFETCH_GLOBAL(StockSplitService*, splitService);
        QFETCH_GLOBAL(domain_id, securityId);
        QFETCH_GLOBAL(domain_id, securityId2);
        auto splitId = addSplit(driver, securityId, QDate{2010, 2, 15}, 1, 2);
        addSplit(driver, securityId, QDate{2015, 12, 15}, 1, 3);
        addSplit(driver, securityId2, QDate{2019, 10, 31}, 3, 2);

        auto result = splitService->getAll();

        QCOMPARE(result.size(), 3);
        auto split = result.value(splitId);
        QCOMPARE(split->sharesIn.normalize().toString(), "1");
        QCOMPARE(split->sharesOut.normalize().toString(), "2");
    }

    void update_savesData() {
        QFETCH_GLOBAL(QString, driver);
        dbTestCase.resetDatabase(driver);
        QFETCH_GLOBAL(SecurityService*, service);
        auto security = dbTestCase.addSecurity(driver, "security x");
        security->name = "security xyz";
        BulkUpdate<Security> changes{{security}, {}, {}};

        auto result = service->update(changes, TEST_USER);

        auto updated = dbTestCase.loadSecurity(driver, security->id.value());
        QCOMPARE(updated->name, security->name);
    }

    void getAccountSecurities() {
        QFETCH_GLOBAL(SecurityService*, service);
        QFETCH_GLOBAL(domain_id, accountId);
        QFETCH_GLOBAL(domain_id, accountId2);
        QFETCH_GLOBAL(domain_id, securityId);
        QFETCH_GLOBAL(domain_id, securityId2);
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId), {"-1.00"}, {"3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId), {"1.00"}, {"-3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId2), {"-10.00"}, {"5"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, {}, securityId), {"-11.00"}, {"2"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, {}, securityId), {"-6.00"}, {"1"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, {}, securityId2), {"-6.00"}, {"4"});

        const auto result = service->getAccountSecurities();

        const auto summaries = forAccount(result.values(), accountId);
        QCOMPARE(summaries.size(), 1);
        verifyAccountSecurity(summaries.first(), securityId2, "5", "10.00", 1);
        const auto summaries2 = forAccount(result.values(), accountId2);
        QCOMPARE(summaries2.size(), 2);
        verifyAccountSecurity(summaries2[0], securityId, "3", "17.00", 2);
        verifyAccountSecurity(summaries2[1], securityId2, "4", "6.00", 1);
    }

    void getAccountSecuritiesByAccountAndSecurity() {
        QFETCH_GLOBAL(SecurityService*, service);
        QFETCH_GLOBAL(domain_id, accountId);
        QFETCH_GLOBAL(domain_id, accountId2);
        QFETCH_GLOBAL(domain_id, securityId);
        QFETCH_GLOBAL(domain_id, securityId2);
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId), {"-1.00"}, {"3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId), {"1.00"}, {"-3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, {}, securityId2), {"-10.00"}, {"5"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, {}, securityId), {"-11.00"}, {"2"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, {}, securityId), {"-6.00"}, {"1"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, {}, securityId2), {"-6.00"}, {"4"});

        const auto result = service->getAccountSecurities({accountId}, {securityId2});

        const auto summaries = forAccount(result.values(), accountId);
        QCOMPARE(summaries.size(), 1);
        verifyAccountSecurity(summaries.first(), securityId2, "5", "10.00", 1);
        const auto summaries2 = forAccount(result.values(), accountId2);
        QCOMPARE(summaries2.size(), 0);
    }

    void cleanup() {
        QFETCH_GLOBAL(QString, driver);
        Connection conn(dbTestCase.connectionPool(driver));
        QSqlQuery query{conn.db};
        query.exec("delete from tx_detail");
        query.exec("delete from tx");
    }
};

QTEST_GUILESS_MAIN(TestSecurityService)
#include "test_securityservice.moc"
