#include <QTest>
#include "dbtestcase.h"
#include "service/database/transactiondetaildao.h"
#include "service/transactionservice.h"

#define TX_UPDATES(...) QList<Transaction*>{__VA_ARGS__}
#define TX_ADDS(...) QList<const PendingTransaction*>{__VA_ARGS__}
#define TX_DELETES(...) QList<const Transaction*>{__VA_ARGS__}

#define DETAIL_UPDATES(...) QList<TransactionDetail*>{__VA_ARGS__}
#define DETAIL_ADDS(...) QList<const TransactionDetail*>{__VA_ARGS__}
#define DETAIL_DELETES(...) QList<const TransactionDetail*>{__VA_ARGS__}

template<class T>
T *find(domain_id id, QList<T*> list) {
    for (auto item : list) if (item->id.value() == id) return item;
    return nullptr;
}

template<class T, class IdType = QVariant>
T *findBy(IdType id, IdType (std::remove_cv<T>::type::*field), QList<T*> list) {
    for (auto item : list) if (item->*field == id) return item;
    return nullptr;
}

class TestTransactionService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

    DbTestCase::TxDetails saveTransaction(QList<const char*> amounts) {
        QFETCH_GLOBAL(domain_id, accountId);
        return dbTestCase.saveTransaction(factory::transaction(accountId), amounts);
    }

    QList<DbTestCase::TxDetails> saveTransfer(QList<const char*> amounts) {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(domain_id, accountId);
        QFETCH_GLOBAL(domain_id, altAccountId);
        return dbTestCase.saveTransfer(driver, accountId, altAccountId, amounts);
    }

    const Transaction* loadTransaction(const domain_id id) {
        QFETCH_GLOBAL(QString, driver);
        Connection conn(dbTestCase.connectionPool(driver));
        auto result = dbTestCase.transactionDao(driver).get(conn.db, QList{id});
        dbTestCase.transactions.append(result.values());
        return result.value(id);
    }

    const TransactionDetail* loadDetail(const domain_id id) {
        QFETCH_GLOBAL(QString, driver);
        Connection conn(dbTestCase.connectionPool(driver));
        auto result = dbTestCase.detailDao(driver).get(conn.db, QList{id});
        dbTestCase.details.append(result.values());
        return result.value(id);
    }

    PendingTransaction* unsavedTransaction(QList<const char*> amounts) {
        QFETCH_GLOBAL(domain_id, accountId);
        QFETCH_GLOBAL(domain_id, payeeId);
        return factory::pendingTransaction(accountId, amounts, payeeId);
    }

    PendingTransaction* unsavedTransfer(QList<const char*> amounts) {
        QFETCH_GLOBAL(domain_id, altAccountId);
        auto tx = unsavedTransaction(amounts);
        tx->details.at(0)->transferAccountId = altAccountId;
        return tx;
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<TransactionService*>("service");
        QTest::addColumn<domain_id>("accountId");
        QTest::addColumn<domain_id>("altAccountId");
        QTest::addColumn<domain_id>("altAccountId2");
        QTest::addColumn<domain_id>("payeeId");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto &txDao = dbTestCase.transactionDao(driver);
            auto &detailDao = dbTestCase.detailDao(driver);
            auto service = new TransactionService{dbTestCase.connectionPool(driver), txDao, detailDao};
            auto companyId = dbTestCase.addCompany(driver, "Bank 1");
            auto accountId = dbTestCase.addAccount(driver, "Account 1", AccountType::bank.code, companyId)->id.value();
            auto altAccountId = dbTestCase.addAccount(driver, "Account 2", AccountType::bank.code, companyId)->id.value();
            auto altAccountId2 = dbTestCase.addAccount(driver, "Account 3", AccountType::bank.code, companyId)->id.value();
            auto payeeId = dbTestCase.addPayee(driver, "Payee 1");
            QTest::newRow(driver.toLocal8Bit()) << driver << service << accountId << altAccountId << altAccountId2 << payeeId;
        }
    }

    void getByAccount_queriesByAccountId() {
        QFETCH_GLOBAL(TransactionService*, service);
        QFETCH_GLOBAL(domain_id, accountId);

        service->getAll(accountId);
    }

    void update_addsNewTransaction() {
        QFETCH_GLOBAL(TransactionService*, service);
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(unsavedTransaction({"1.00", "2.00"})), TX_DELETES(),
            DETAIL_UPDATES(), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.transactions.size(), 1);
        auto tx = result.transactions.at(0);
        QCOMPARE(tx->detailIds.size(), 2);
        QCOMPARE(result.details.size(), 2);
        QVERIFY(tx->id.has_value());
        QVERIFY(tx->detailIds.contains(result.details.at(0)->id.value()));
        QVERIFY(tx->detailIds.contains(result.details.at(1)->id.value()));
        QCOMPARE(result.details.at(0)->transactionId, tx->id.value());
        QCOMPARE(result.details.at(0)->transactionId, tx->id.value());
    }

    void update_addsNewTransfer() {
        QFETCH_GLOBAL(TransactionService*, service);
        QFETCH_GLOBAL(domain_id, accountId);
        QFETCH_GLOBAL(domain_id, altAccountId);
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(unsavedTransfer({"1.00"})), TX_DELETES(),
            DETAIL_UPDATES(), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.details.size(), 2);
        QCOMPARE(result.details.at(0)->relatedDetailId, result.details.at(1)->id);
        QCOMPARE(result.details.at(1)->relatedDetailId, result.details.at(0)->id);
        QCOMPARE(result.transactions.size(), 2);
        auto tx = findBy(accountId, &Transaction::accountId, result.transactions);
        QVERIFY(tx);
        auto detail = findBy(tx->id.value(), &TransactionDetail::transactionId, result.details);
        QVERIFY(detail);
        QVERIFY(tx->detailIds.contains(detail->id));
        auto relatedTx = findBy(altAccountId, &Transaction::accountId, result.transactions);
        QVERIFY(relatedTx);
        QVERIFY(relatedTx->id != tx->id);
        auto relatedDetail = findBy(relatedTx->id.value(), &TransactionDetail::transactionId, result.details);
        QVERIFY(relatedDetail);
        QVERIFY(relatedTx->detailIds.contains(relatedDetail->id));
        QCOMPARE(relatedDetail->relatedDetailId, detail->id);
        QCOMPARE(detail->relatedDetailId, relatedDetail->id);
        auto updatedDetail = loadDetail(detail->id.value());
        QCOMPARE(updatedDetail->relatedDetailId, relatedDetail->id);
    }

    void update_updatesTransactionAndDetail() {
        QFETCH_GLOBAL(TransactionService*, service);
        auto [tx, details] = saveTransaction(QList{"1.00", "2.00"});
        auto newDetail = factory::detail("3.45");
        newDetail->transactionId = tx->id.value();
        tx->payeeId = QVariant{};
        tx->memo = "tx comment";
        auto detail0 = details.at(0);
        detail0->amount = DECIMAL_VARIANT("3.21");
        detail0->memo = "detail comment";
        TransactionUpdate changes{
            TX_UPDATES(tx), TX_ADDS(), TX_DELETES(),
            DETAIL_UPDATES(detail0), DETAIL_ADDS(newDetail), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.transactions.size(), 1);
        auto detailIds = getEntityIds(details);
        detailIds.append(changes.detailAdds.at(0)->id.value());
        QCOMPARE(result.transactions.at(0)->detailIds.size(), 3);
        QCOMPARE(result.transactions.at(0)->detailIds, detailIds);
        auto updatedTx = loadTransaction(tx->id.value());
        QVERIFY(updatedTx->payeeId.isNull());
        QCOMPARE(updatedTx->memo, tx->memo);
        auto updatedDetail = loadDetail(detail0->id.value());
        QCOMPARE(updatedDetail->amount, detail0->amount);
        QCOMPARE(updatedDetail->memo, detail0->memo);
    }

    void update_updatesTransferAmount() {
        QFETCH_GLOBAL(TransactionService*, service);
        auto transfer = saveTransfer({"1.00", "2.00"});
        auto [tx, details] = transfer.at(0);
        details.at(0)->amount = DECIMAL_VARIANT("3.45");
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(),
            DETAIL_UPDATES(details.at(0)), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.details.size(), 2);
        auto resultDetail = find(details.at(0)->relatedDetailId.toLongLong(), result.details);
        QCOMPARE(resultDetail->amount.toString(), "-3.45");
    }

    void update_addsRelatedTransactionAndDetail() {
        QFETCH_GLOBAL(TransactionService*, service);
        QFETCH_GLOBAL(domain_id, altAccountId);
        auto [tx, details] = saveTransaction(QList{"1.00", "2.34"});
        auto detail = details.at(1);
        detail->transferAccountId = altAccountId;
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(),
            DETAIL_UPDATES(detail), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.transactions.size(), 1);
        QVERIFY(result.transactions.at(0)->id != tx->id);
        QCOMPARE(result.details.size(), 2);
        QCOMPARE(result.details.at(0)->id, detail->id);
        auto relatedDetail = result.details.at(1);
        QCOMPARE(result.transactions.at(0)->detailIds, QList<domain_id>{relatedDetail->id.value()});
        QCOMPARE(detail->relatedDetailId, relatedDetail->id);
        QCOMPARE(relatedDetail->relatedDetailId, detail->id);
        QCOMPARE(relatedDetail->amount.toString(), "-2.34");
    }

    void update_updatesRelatedTransactionAccount() {
        QFETCH_GLOBAL(TransactionService*, service);
        QFETCH_GLOBAL(domain_id, altAccountId2);
        auto transfer = saveTransfer({"1.00", "2.00"});
        auto [tx, details] = transfer.at(0);
        auto [relatedTx, relatedDetails] = transfer.at(1);
        details.at(0)->transferAccountId = altAccountId2;
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(),
            DETAIL_UPDATES(details.at(0)), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.transactions.size(), 1);
        QCOMPARE(result.transactions.at(0)->id, relatedTx->id);
        QCOMPARE(result.transactions.at(0)->accountId, altAccountId2);
        auto updatedTx = loadTransaction(relatedTx->id.value());
        QCOMPARE(updatedTx->accountId, altAccountId2);
    }

    void update_removesRelatedDetail() {
        QFETCH_GLOBAL(TransactionService*, service);
        QFETCH_GLOBAL(domain_id, altAccountId);
        auto transfer = saveTransfer({"1.23", "2.34"});
        auto [tx, details] = transfer.at(0);
        auto [relatedTx, relatedDetails] = transfer.at(1);
        details.at(0)->transferAccountId = {};
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(relatedTx),
            DETAIL_UPDATES(), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QVERIFY(result.deletedIds.isEmpty());
        // returns updated related transaction
        QCOMPARE(result.transactions.size(), 1);
        QCOMPARE(result.transactions.at(0)->id, tx->id);
        QCOMPARE(result.transactions.at(0)->detailIds.size(), 1);
        QCOMPARE(result.deletedDetailIds.size(), 1);
        QVERIFY(result.deletedDetailIds.contains(details.at(0)->id));
        QCOMPARE(loadTransaction(relatedTx->id.value()), nullptr);
        QCOMPARE(loadDetail(relatedDetails.at(0)->id.value()), nullptr);
        QCOMPARE(loadDetail(details.at(1)->id.value())->amount.toString(), "2.34");
    }

    void update_removesRelatedTransactionAndDetail() {
        QFETCH_GLOBAL(TransactionService*, service);
        QFETCH_GLOBAL(domain_id, altAccountId);
        auto transfer = saveTransfer({"1.23"});
        auto [tx, details] = transfer.at(0);
        auto [relatedTx, relatedDetails] = transfer.at(1);
        details.at(0)->transferAccountId = {};
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(),
            DETAIL_UPDATES(details), DETAIL_ADDS(), DETAIL_DELETES(),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.deletedIds.size(), 1);
        QVERIFY(result.deletedIds.contains(relatedTx->id));
        QCOMPARE(result.deletedDetailIds.size(), 1);
        QVERIFY(result.deletedDetailIds.contains(relatedDetails.at(0)->id));
        QCOMPARE(loadTransaction(relatedTx->id.value()), nullptr);
        QCOMPARE(loadDetail(relatedDetails.at(0)->id.value()), nullptr);
        QCOMPARE(result.details.at(0)->relatedDetailId, QVariant{});
        QVERIFY(loadDetail(details.at(0)->id.value()) != nullptr);
    }

    void update_deletesTransactionAndDetails() {
        QFETCH_GLOBAL(TransactionService*, service);
        auto [tx1, details1] = saveTransaction(QList{"1.00", "2.00", "1.11", "2.22"});
        auto [tx2, details2] = saveTransaction(QList{"3.00"});
        auto transfer1 = saveTransfer({"4.00"}); // delete transfer tx
        auto [tx3, details3] = transfer1.at(0);
        auto [tx4, details4] = transfer1.at(1);
        auto transfer2 = saveTransfer({"5.00", "6.00"}); // delete transfer detail
        auto [tx5, details5] = transfer2.at(0);
        auto [tx6, details6] = transfer2.at(1);
        TransactionUpdate changes{
            TX_UPDATES(), TX_ADDS(), TX_DELETES(tx2, tx4),
            DETAIL_UPDATES(), DETAIL_ADDS(), DETAIL_DELETES(details1.at(0), details1.at(2), details1.at(3), details5.at(0)),
        };

        auto result = service->update(changes, TEST_USER);

        QCOMPARE(result.deletedIds.size(), 2);
        QVERIFY(result.deletedIds.contains(tx3->id));
        QVERIFY(result.deletedIds.contains(tx6->id));
        QCOMPARE(result.deletedDetailIds.size(), 1); // related detail for deleted transfer
        QCOMPARE(result.deletedDetailIds.at(0), details3.at(0)->id);
        auto resultTx = find(tx1->id.value(), result.transactions);
        QCOMPARE(resultTx->detailIds.size(), 1);
        QCOMPARE(loadTransaction(tx2->id.value()), nullptr); // deleted transaction
        QCOMPARE(loadDetail(details2.at(0)->id.value()), nullptr);
        QCOMPARE(loadTransaction(tx3->id.value()), nullptr); // deleted transfer
        QCOMPARE(loadTransaction(tx4->id.value()), nullptr);
        QCOMPARE(loadDetail(details3.at(0)->id.value()), nullptr);
        QCOMPARE(loadDetail(details4.at(0)->id.value()), nullptr);
        QCOMPARE(loadDetail(details1.at(0)->id.value()), nullptr); // deleted detail
        QCOMPARE(loadDetail(details5.at(0)->id.value()), nullptr); // deleted transfer detail
        QVERIFY(loadDetail(details5.at(1)->id.value()) != nullptr);
        QCOMPARE(loadDetail(details6.at(0)->id.value()), nullptr);
        QVERIFY(loadTransaction(tx5->id.value()) != nullptr);
        QCOMPARE(loadTransaction(tx6->id.value()), nullptr);
    }

    void cleanup() {
        dbTestCase.cleanup();
    }

    void cleanupTestCase() {
        // TODO shutdown connection pool?
    }
};

QTEST_GUILESS_MAIN(TestTransactionService)
#include "test_transactionservice.moc"
