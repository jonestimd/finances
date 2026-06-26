#include <QTest>
#include "dbtestcase.h"
#include "service/securityservice.h"

class TestSecurityService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

    QString normalize(const Security *security, QVariant(Security::*member)) {
        auto value = (security->*member).value<QDecNumber>();
        return value.normalize().toString();
    }

    void verifySecurity(const Security *security, int txCount, const char *shares, const char *costBasis) {
        QCOMPARE(security->transactions.toInt(), txCount);
        QCOMPARE(normalize(security, &Security::shares), shares);
        QCOMPARE(normalize(security, &Security::costBasis), costBasis);
    }

    void addSplit(const QString &driver, QVariant securityId, QDate date, int sharesIn, int sharesOut) {
        Connection conn(dbTestCase.connectionPool(driver));
        StockSplit split{};
        split.securityId = securityId;
        split.date = date;
        split.sharesIn = sharesIn;
        split.sharesOut = sharesOut;
        dbTestCase.stockSplitDao(driver).add(conn.db, {&split}, TEST_USER);
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<SecurityService*>("service");
        QTest::addColumn<domain_id>("accountId");
        QTest::addColumn<domain_id>("accountId2");
        QTest::addColumn<domain_id>("securityId");
        QTest::addColumn<domain_id>("securityId2");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto &dao = dbTestCase.securityDao(driver);
            auto accountId = dbTestCase.addAccount(driver, "account 1", AccountType::bank.code)->id.value();
            auto accountId2 = dbTestCase.addAccount(driver, "account 2", AccountType::bank.code)->id.value();
            auto securityId = dbTestCase.addSecurity(driver, "security 1")->id.value();
            auto securityId2 = dbTestCase.addSecurity(driver, "security 2")->id.value();
            auto service = new SecurityService{dbTestCase.connectionPool(driver), dao, dbTestCase.stockSplitDao(driver)};
            QTest::newRow(driver.toLocal8Bit()) << driver << service << accountId << accountId2 << securityId << securityId2;
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
        dbTestCase.saveTransaction(factory::transaction(accountId, QVariant{}, securityId), {"-1.23", "-2.00"}, {"3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, QVariant{}, securityId), {"-10.45"}, {"6"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, QVariant{}, securityId2), {"-2.00"}, {"5"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, QVariant{}, securityId2), {"20.00"}, {"-1"});

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
        dbTestCase.saveTransaction(factory::transaction(accountId, QVariant{}, securityId, tx1Date), {"-1.00"}, {"3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, QVariant{}, securityId, tx2Date), {"-1.00"}, {"5"});
        addSplit(driver, securityId, tx1Date, 1, 2);

        auto result = service->getAll();

        verifySecurity(result.value(securityId), 2, "11", "2");
    }

    void getSplits_returnsAllSplits() {
        QFETCH_GLOBAL(QString, driver);
        dbTestCase.resetDatabase(driver);
        QFETCH_GLOBAL(SecurityService*, service);
        QFETCH_GLOBAL(domain_id, securityId);
        QFETCH_GLOBAL(domain_id, securityId2);
        addSplit(driver, securityId, QDate{2010, 2, 15}, 1, 2);
        addSplit(driver, securityId, QDate{2015, 12, 15}, 1, 3);
        addSplit(driver, securityId2, QDate{2019, 10, 31}, 3, 2);

        auto result = service->getSplits();

        QCOMPARE(result.size(), 3);
        auto split = result.values().constFirst();
        QVERIFY(split->sharesIn.canConvert<QDecNumber>());
        QVERIFY(split->sharesOut.canConvert<QDecNumber>());
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
};

QTEST_GUILESS_MAIN(TestSecurityService)
#include "test_securityservice.moc"
