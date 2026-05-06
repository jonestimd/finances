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

    void resetDatabase(const QString &driver) {
        Connection conn(dbTestCase.connectionPool(driver));
        QSqlQuery query{conn.db};
        query.exec("delete from tx_detail");
        query.exec("delete from tx");
        query.exec("delete from stock_split");
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<SecurityService*>("service");
        QTest::addColumn<QVariant>("payeeId");
        QTest::addColumn<QVariant>("accountId");
        QTest::addColumn<QVariant>("accountId2");
        QTest::addColumn<QVariant>("securityId");
        QTest::addColumn<QVariant>("securityId2");
        QVariant payeeId{};
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto &dao = dbTestCase.securityDao(driver);
            auto accountId = dbTestCase.addAccount(driver, "account 1", AccountType::bank.code)->id;
            auto accountId2 = dbTestCase.addAccount(driver, "account 2", AccountType::bank.code)->id;
            auto securityId = dbTestCase.addSecurity(driver, "security 1");
            auto securityId2 = dbTestCase.addSecurity(driver, "security 2");
            auto service = new SecurityService{dbTestCase.connectionPool(driver), dao};
            QTest::newRow(driver.toLocal8Bit()) << driver << service << payeeId << accountId << accountId2 << securityId << securityId2;
        }
    }

    void getAll_returnsTransactionSummary() {
        QFETCH_GLOBAL(QString, driver);
        resetDatabase(driver);
        QFETCH_GLOBAL(SecurityService*, service);
        QFETCH_GLOBAL(QVariant, accountId);
        QFETCH_GLOBAL(QVariant, accountId2);
        QFETCH_GLOBAL(QVariant, securityId);
        QFETCH_GLOBAL(QVariant, securityId2);
        dbTestCase.saveTransaction(factory::transaction(accountId, QVariant{}, securityId), {"-1.23", "-2.00"}, {"3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, QVariant{}, securityId), {"-10.45"}, {"6"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, QVariant{}, securityId2), {"-2.00"}, {"5"});
        dbTestCase.saveTransaction(factory::transaction(accountId2, QVariant{}, securityId2), {"20.00"}, {"-1"});

        auto result = service->getAll();

        verifySecurity(result.value(securityId.toLongLong()), 2, "9", "11.68");
        verifySecurity(result.value(securityId2.toLongLong()), 2, "4", "2");
    }

    void getAll_adjustsSharesForSplits() {
        QFETCH_GLOBAL(QString, driver);
        resetDatabase(driver);
        QFETCH_GLOBAL(SecurityService*, service);
        QFETCH_GLOBAL(QVariant, accountId);
        QFETCH_GLOBAL(QVariant, securityId);
        QDate tx1Date{2010, 2, 15};
        QDate tx2Date = tx1Date.addDays(1);
        dbTestCase.saveTransaction(factory::transaction(accountId, QVariant{}, securityId, tx1Date), {"-1.00"}, {"3"});
        dbTestCase.saveTransaction(factory::transaction(accountId, QVariant{}, securityId, tx2Date), {"-1.00"}, {"5"});
        addSplit(driver, securityId, tx1Date, 1, 2);

        auto result = service->getAll();

        verifySecurity(result.value(securityId.toLongLong()), 2, "11", "2");
    }
};

QTEST_GUILESS_MAIN(TestSecurityService)
#include "test_securityservice.moc"
