#include <QSignalSpy>
#include <QTest>

#include "service/tests/dbtestcase.h"
#include "ui/uicontext.h"
#include "ui/widget/relationeditor.h"
#include "ui/widget/transactionswindow.h"

#define COMPANY_NAME "Bank 1"
#define ACCOUNT_NAME "Checking"
#define ALT_ACCOUNT_NAME "Savings"
#define PAYEE_NAME "Payee 1"
#define CATEGORY_NAME "Category 1"

#define INITIAL_TRANSACTION_COUNT 3
#define ALT_INITIAL_TRANSACTION_COUNT 1

struct WindowHolder {
    TransactionsWindow* const window;

    WindowHolder(TransactionsWindow* window) : window{window} {}
    ~WindowHolder() {
        window->model()->clearChanges();
        QVERIFY(window->close());
        delete window;
    }

    TransactionTableModel* model() {
        return window->model();
    }
};

class TestTransactionsWindow : public QObject {
    Q_OBJECT
    const char* driver = SQLITE_DRIVER;

    DbTestCase dbTestCase{};
    ServiceContext services{dbTestCase.connectionPool(driver)};
    DataStore dataStore{&services};
    UiContext uiContext{&dataStore};

    TransactionsWindow *window;
    TreeView* treeView;
    QVariant companyId;
    QVariant accountId;
    QVariant altAccountId;
    QVariant payeeId;
    QVariant categoryId;

    QSignalSpy *accountUpdatedSpy;

private:
    void waitForDataLoaded(TransactionsWindow *win) {
        auto spy = QSignalSpy(win->model(), SIGNAL(dataLoaded()));
        QVERIFY(spy.isValid());
        QVERIFY(spy.wait());
        QVERIFY(win->findChild<TreeView*>()->isEnabled());
    }

    TransactionsWindow* openWindow(const QVariant &accountId) {
        TransactionsWindow* window = uiContext.showTransactions(accountId.toLongLong());
        waitForDataLoaded(window);
        return window;
    }

    void addDetail() {
        QTest::keyClick(treeView, Qt::Key_N, Qt::ControlModifier);
        QTest::keyClick(treeView, Qt::Key_Escape);
    }

    void enterText(const char* text) {
        QSignalSpy editSpy(treeView->itemDelegate(), &QAbstractItemDelegate::closeEditor);
        QVERIFY(editSpy.isValid());
        QTest::keyClick(treeView, text[0]);
        auto editor = qobject_cast<QLineEdit*>(window->focusWidget());
        QTest::keyClicks(editor, QString{text}.slice(1));
        QTest::keyClick(editor, Qt::Key_Enter);
        QVERIFY(editSpy.wait());
    }

    void selectValue(const char* text) {
        QSignalSpy editSpy(treeView->itemDelegate(), &QAbstractItemDelegate::closeEditor);
        QVERIFY(editSpy.isValid());
        QTest::keyClick(treeView, text[0]);
        auto editor = qobject_cast<RelationEditor*>(window->focusWidget());
        QTest::keyClicks(editor, QString{text}.slice(1));
        QTest::keySequence(editor, {Qt::Key_Enter, Qt::Key_Enter});
        QVERIFY(editSpy.wait());
    }

    void fillTransaction(const char* refNumber, const char* payee, const char* description) {
        if (window->focusWidget() != treeView) focusWindow(treeView);
        QTRY_COMPARE(window->focusWidget(), treeView);
        QTest::keyClick(treeView, Qt::Key_Tab);
        enterText(refNumber);
        QTest::keyClick(treeView, Qt::Key_Tab);
        selectValue(payee);
        QTest::keyClick(treeView, Qt::Key_Tab);
        enterText(description);
    }

    void fillDetail(const char* category, const char* amount = nullptr) {
        if (window->focusWidget() != treeView) focusWindow(treeView);
        QTRY_COMPARE(window->focusWidget(), treeView);
        if (!treeView->currentIndex().parent().isValid()) QTest::keyClick(treeView, Qt::Key_Down);
        QTest::keySequence(treeView, {Qt::Key_Home, Qt::Key_Tab, Qt::Key_Tab});
        selectValue(category);
        if (amount) {
            QTest::keySequence(treeView, {Qt::Key_Tab, Qt::Key_Tab});
            enterText(amount);
        }
    }

    void verifyTransaction(const Transaction* tx, QVariant refNumber, QVariant payeeId, QVariant description) {
        QCOMPARE(tx->referenceNumber, refNumber);
        QCOMPARE(tx->payeeId, payeeId);
        QCOMPARE(tx->memo, description);
    }

    void verifyDetail(QVariant detailId, QVariant categoryId, QVariant transferAccountId, QVariant amount) {
        verifyDetail(dataStore.transactionStore->detailStore.value(detailId), categoryId, transferAccountId, amount);
    }

    void verifyPendingTransaction(const TransactionsWindow *window) {
        auto pendingTx = window->model()->unsavedAdds().at(0);
        verifyTransaction(pendingTx, QVariant{}, QVariant{}, QVariant{});
        QCOMPARE(pendingTx->details.size(), 1);
        verifyDetail(pendingTx->details.at(0), QVariant{}, QVariant{}, QVariant{});
    }

    void verifyDetail(const TransactionDetail* detail, QVariant categoryId, QVariant transferAccountId, QVariant amount) {
        QCOMPARE(detail->categoryId, categoryId);
        QCOMPARE(detail->transferAccountId, transferAccountId);
        if (amount.isNull()) QVERIFY(detail->amount.isNull());
        else QCOMPARE(detail->amount.toString(), amount);
    }

    void focusWindow(QWidget* widget) {
        widget->raise();
        widget->activateWindow();
        widget->setFocus();
        QVERIFY(QTest::qWaitForWindowFocused(window));
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        companyId = dbTestCase.addCompany(driver, COMPANY_NAME);
        accountId = dbTestCase.addAccount(driver, ACCOUNT_NAME, AccountType::bank.code, companyId)->id;
        altAccountId = dbTestCase.addAccount(driver, ALT_ACCOUNT_NAME, AccountType::bank.code, companyId)->id;
        payeeId = dbTestCase.addPayee(driver, PAYEE_NAME);
        categoryId = dbTestCase.addCategory(driver, CATEGORY_NAME);
    }

    void init() {
        dbTestCase.resetDatabase(driver);
        dbTestCase.saveTransaction(driver, factory::transaction(accountId), {"23.45"});
        dbTestCase.saveTransaction(driver, factory::transaction(accountId), {"34.56"});
        dbTestCase.saveTransfer(driver, altAccountId, accountId, {"78.90", "567.89"});
        window = openWindow(accountId);
        treeView = window->findChild<TreeView*>();
        accountUpdatedSpy = new QSignalSpy(dataStore.transactionStore, SIGNAL(accountUpdated(qlonglong)));
        QVERIFY(accountUpdatedSpy->isValid());
    }

    void addTransaction() {
        QCOMPARE(window->focusWidget(), treeView);
        fillTransaction("123", PAYEE_NAME, "description");
        fillDetail(CATEGORY_NAME, "12.34");

        QTest::keyClick(treeView, Qt::Key_Enter);
        QVERIFY(accountUpdatedSpy->wait());
        QTRY_COMPARE(window->focusWidget(), treeView);

        QCOMPARE(dataStore.transactionStore->transactionIds(accountId.toLongLong()).size(), INITIAL_TRANSACTION_COUNT+1);
        QCOMPARE(window->model()->rowCount(), INITIAL_TRANSACTION_COUNT+2);
        auto tx = window->model()->getRow(window->model()->index(INITIAL_TRANSACTION_COUNT, 0));
        verifyTransaction(tx, "123", payeeId, "description");
        verifyDetail(tx->detailIds.at(0), categoryId, QVariant{}, "12.34");
        QVERIFY(window->model()->unsavedDetailAdds().isEmpty());
        // should reset the new transaction
        verifyPendingTransaction(window);
    }

    void deleteTransaction_adjustsErrors() {
        QCOMPARE(window->focusWidget(), treeView);
        fillDetail(CATEGORY_NAME);
        QCOMPARE(window->model()->isValid(), false);
        treeView->setCurrentIndex(treeView->model()->index(0, 0));

        QTest::keySequence(treeView, {Qt::Key_Delete, Qt::Key_Enter});
        QVERIFY(accountUpdatedSpy->wait());

        auto model = window->model();
        QVERIFY(!model->transactionIsValid(model->index(model->rowCount()-1, 0)));
        QCOMPARE(model->rowCount(), INITIAL_TRANSACTION_COUNT);
        QCOMPARE(dataStore.transactionStore->transactionIds(accountId.toLongLong()).count(), INITIAL_TRANSACTION_COUNT-1);
    }

    void addTransfer_updatesRelatedWindows() {
        WindowHolder window2(openWindow(altAccountId));
        focusWindow(treeView);
        QCOMPARE(window2.model()->rowCount(), ALT_INITIAL_TRANSACTION_COUNT+1);
        QCOMPARE(window->focusWidget(), treeView);
        fillTransaction("123", PAYEE_NAME, "description");
        fillDetail(ALT_ACCOUNT_NAME, "2.34");
        addDetail();
        fillDetail(CATEGORY_NAME, "321.43");
        if (window->focusWidget() != treeView) focusWindow(treeView); // FIXME why is focus moving to filter input?

        QTest::keyClick(treeView, Qt::Key_Enter);
        QVERIFY(accountUpdatedSpy->wait());
        QTRY_COMPARE(window->focusWidget(), treeView);

        QCOMPARE(window2.model()->rowCount(), ALT_INITIAL_TRANSACTION_COUNT+2);
        auto tx = window2.model()->getRow(window2.model()->index(ALT_INITIAL_TRANSACTION_COUNT, 0));
        verifyTransaction(tx, QVariant{}, payeeId, "description");
        verifyDetail(tx->detailIds.at(0), QVariant{}, accountId, "-2.34");
        verifyPendingTransaction(window);
        verifyPendingTransaction(window2.window);
    }

    void updateDetail_updatesRelatedWindows() {
        WindowHolder window2(openWindow(altAccountId));
        focusWindow(treeView);
        treeView->setCurrentIndex(treeView->model()->index(0, 0));
        QCOMPARE(window->focusWidget(), treeView);
        fillDetail(ALT_ACCOUNT_NAME, "98.76");
        if (window->focusWidget() != treeView) focusWindow(treeView); // FIXME why is focus moving to filter input?

        QTest::keyClick(treeView, Qt::Key_S, Qt::ControlModifier);
        QVERIFY(accountUpdatedSpy->wait());
        QTRY_COMPARE(window->focusWidget(), treeView);
        QTest::qWait(100);

        QCOMPARE(window2.model()->rowCount(), ALT_INITIAL_TRANSACTION_COUNT+2);
        auto tx = window2.model()->getRow(window2.model()->index(ALT_INITIAL_TRANSACTION_COUNT, 0));
        verifyTransaction(tx, QVariant{}, QVariant{}, QVariant{});
        verifyDetail(tx->detailIds.at(0), QVariant{}, accountId, "-98.76");
    }

    void deleteTransfer_updatesRelatedWindows() {
        WindowHolder window2(openWindow(altAccountId));
        focusWindow(treeView);

        treeView->setCurrentIndex(treeView->model()->index(INITIAL_TRANSACTION_COUNT-1, 0));
        QTest::keySequence(treeView, {Qt::Key_Delete, Qt::Key_Enter});
        QVERIFY(accountUpdatedSpy->wait());

        auto model = window->model();
        QVERIFY(model->isPendingAdd(model->index(model->rowCount()-1, 0)));
        QCOMPARE(model->rowCount(), INITIAL_TRANSACTION_COUNT);
        QCOMPARE(dataStore.transactionStore->transactionIds(accountId.toLongLong()).count(), INITIAL_TRANSACTION_COUNT-1);
        auto model2 = window2.model();
        QCOMPARE(model2->rowCount(), ALT_INITIAL_TRANSACTION_COUNT+1);
        auto tx = model2->getRow(model2->index(0, 0));
        qDebug() << tx->detailIds.size();
        QCOMPARE(model2->rowCount(model2->index(0, 0)), 1);
    }

    void cleanup() {
        window->model()->clearChanges();
        QVERIFY(window->close());
        delete window;
        window = nullptr;
        treeView = nullptr;
        delete accountUpdatedSpy;
        accountUpdatedSpy = nullptr;
        dbTestCase.cleanup();
    }
};

QTEST_MAIN(TestTransactionsWindow)
#include "test_transactionswindow.moc"