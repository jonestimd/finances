#include <QTest>
#include "dbtestcase.h"
#include "service/accountservice.h"

#define DEFAULT_COMPANY_COUNT 1

class TestAccountService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

    QString getName(const char *accountName) {
        auto testName = QTest::currentTestFunction();
        return QString{"%0:%1"}.arg(testName, accountName);
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<AccountService*>("service");
        QTest::addColumn<QVariant>("companyId");
        QTest::addColumn<QVariant>("payeeId");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto &accountDao = dbTestCase.accountDao(driver);
            auto &companyDao = dbTestCase.companyDao(driver);
            auto service = new AccountService{dbTestCase.connectionPool(driver), accountDao, companyDao};
            auto companyId = dbTestCase.addCompany(driver, "company");
            auto payeeId = dbTestCase.addPayee(driver, "payee");
            QTest::newRow(driver.toLocal8Bit())
                << driver << service << companyId << payeeId;
        }
    }

    void getAll_returnsTransactionSummary() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(AccountService*, service);
        auto account = dbTestCase.addAccount(driver, "account", "BANK");
        // for sqlite3: use amounts that will result in rounding errors if decimal extension is not loaded
        dbTestCase.saveTransaction(factory::transaction(account->id), {"1.23"});
        dbTestCase.saveTransaction(factory::transaction(account->id), {"2.34", "-5.00"});

        auto result = service->getAll();

        auto accountSummary = result.value(account->id.toLongLong());
        QCOMPARE(accountSummary->transactions, 2);
        QCOMPARE(accountSummary->balance, DECIMAL_VARIANT("-1.43"));
    }

    void update_savesData() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(AccountService*, service);
        auto account = dbTestCase.addAccount(driver, "account", "BANK");
        account->name = "new name";
        account->description = "more info";
        account->accountNumber = "123-456";
        account->closed = true;
        BulkUpdate<Account> changes{{account}, {}, {}};

        QHash<qlonglong, const Company*> companies{};
        auto result = service->update(changes, TEST_USER, companies);

        auto updated = dbTestCase.loadAccount(driver, account->id);
        QCOMPARE(updated->name, account->name);
        QCOMPARE(updated->description, account->description);
        QCOMPARE(updated->accountNumber, account->accountNumber);
        QCOMPARE(updated->closed, account->closed);
        QCOMPARE(account->version, 1);
        QCOMPARE(companies.size(), DEFAULT_COMPANY_COUNT);
    }

    void cleanup() {
        dbTestCase.cleanup();
    }
};

QTEST_GUILESS_MAIN(TestAccountService)
#include "test_accountservice.moc"
