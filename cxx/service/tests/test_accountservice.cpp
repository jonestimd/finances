#include <QTest>
#include "dbtestcase.h"
#include "service/accountservice.h"

#define DEFAULT_COMPANY_COUNT 1

class TestAccountService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

    QList<const Account*> accounts{};

    QString getName(const char *accountName) {
        auto testName = QTest::currentTestFunction();
        return QString{"%0:%1"}.arg(testName, accountName);
    }

    Account *addAccount(QString driver, const char *name, const QString &type, const QVariant companyId = QVariant{}) {
        auto conn = Connection(dbTestCase.connectionPool(driver));
        Account *account = new Account;
        account->name = getName(name);
        account->type = type;
        account->companyId = companyId;
        accountDao.add(conn.db, QList{account}, TEST_USER);
        this->accounts.append(account);
        return account;
    }

    const Account *loadAccount(QString driver, const QVariant &id) {
        auto conn = Connection(dbTestCase.connectionPool(driver));
        auto rows = accountDao.get(conn.db, {id});
        accounts.append(rows.values());
        return rows.value(id.toLongLong());
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<AccountService*>("service");
        QTest::addColumn<QVariant>("companyId");
        QTest::addColumn<QVariant>("payeeId");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto service = new AccountService{dbTestCase.connectionPool(driver)};
            auto companyId = dbTestCase.addCompany(driver, "company");
            auto payeeId = dbTestCase.addPayee(driver, "payee");
            QTest::newRow(driver.toLocal8Bit()) << driver << service << companyId << payeeId;
        }
    }

    void getAll_returnsTransactionSummary() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(AccountService*, service);
        auto account = addAccount(driver, "account", "BANK");
        // for sqlite3: use amounts that will result in rounding errors if decimal extension is not loaded
        dbTestCase.saveTransaction(account->id, {"1.23"});
        dbTestCase.saveTransaction(account->id, {"2.34", "-5.00"});

        auto result = service->getAll();

        auto accountSummary = result.value(account->id.toLongLong());
        QCOMPARE(accountSummary->transactions, 2);
        QCOMPARE(accountSummary->balance, DECIMAL_VARIANT("-1.43"));
    }

    void update_savesData() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(AccountService*, service);
        auto account = addAccount(driver, "account", "BANK");
        account->name = "new name";
        account->description = "more info";
        account->accountNumber = "123-456";
        account->closed = true;
        BulkUpdate<Account> changes{{account}, {}, {}};

        QHash<qlonglong, const Company*> companies{};
        auto result = service->update(changes, TEST_USER, companies);

        auto updated = loadAccount(driver, account->id);
        QCOMPARE(updated->name, account->name);
        QCOMPARE(updated->description, account->description);
        QCOMPARE(updated->accountNumber, account->accountNumber);
        QCOMPARE(updated->closed, account->closed);
        QCOMPARE(account->version, 1);
        QCOMPARE(companies.size(), DEFAULT_COMPANY_COUNT);
    }

    void cleanup() {
        for (auto account : std::as_const(accounts)) delete account ;
        accounts.clear();
    }
};

QTEST_GUILESS_MAIN(TestAccountService)
#include "test_accountservice.moc"
