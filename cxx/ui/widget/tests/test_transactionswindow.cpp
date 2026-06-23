#include <QSignalSpy>
#include <QTest>
#include <QTimer>

#include "service/tests/dbtestcase.h"
#include "ui/uicontext.h"
#include "ui/widget/entityselectiondialog.h"
#include "ui/widget/relationeditor.h"
#include "ui/widget/transactionswindow.h"
#include "windowtest.h"

#define COMPANY_NAME "Bank 1"
#define ACCOUNT_NAME "Checking"
#define ALT_ACCOUNT_NAME "Savings"
#define SECURITY_ACCOUNT_NAME "Broker"
#define PAYEE_NAME "Payee 1"
#define ALT_PAYEE_NAME "Payee 2"
#define CATEGORY_NAME "Category 1"
#define ALT_CATEGORY_NAME "Category 2"
#define GROUP_NAME "Group 1"

#define INITIAL_TRANSACTION_COUNT 3
#define ALT_INITIAL_TRANSACTION_COUNT 1

template<class Window, class Model, class View>
struct WindowHolder {
    typedef Window windowType;

    Window* const window;
    View* const view;

    WindowHolder(Window* window)
        : window{window}
        , view{window->template findChild<View*>()}
    {}

    ~WindowHolder() {
        window->model()->clearChanges();
        QVERIFY(window->close());
        delete window;
    }

    void focusWindow() {
        view->raise();
        view->activateWindow();
        view->setFocus();
        QVERIFY(QTest::qWaitForWindowFocused(window));
    }

    void showWindow() {
        window->show();
        QVERIFY(QTest::qWaitForWindowActive(window));
        focusWindow();
        QCOMPARE(window->focusWidget(), view);
    }

    Model* model() {
        return window->model();
    }

    QModelIndex index(int row, int column, QModelIndex parent = QModelIndex{}) {
        return view->model()->index(row, column, parent);
    }

    QVariant data(int row, int column, QModelIndex parent = QModelIndex{}) {
        return view->model()->data(index(row, column, parent));
    }
};

typedef WindowHolder<TransactionsWindow, TransactionTableModel, TreeView> TxWindowHolder;
typedef WindowHolder<AccountsWindow, AccountTableModel, QTableView> AccountWindowHolder;
typedef WindowHolder<PayeesWindow, PayeeTableModel, QTableView> PayeeWindowHolder;
typedef WindowHolder<CategoriesWindow, CategoryTableModel, TreeView> CategoryWindowHolder;
typedef WindowHolder<GroupsWindow, GroupTableModel, QTableView> GroupWindowHolder;
typedef WindowHolder<SecuritiesWindow, SecurityTableModel, QTableView> SecurityWindowHolder;

class TestTransactionsWindow : public QObject {
    Q_OBJECT
    const char* driver = SQLITE_DRIVER;

    DbTestCase dbTestCase{};
    ServiceContext services{dbTestCase.connectionPool(driver)};
    DataStore* dataStore;
    UiContext* uiContext;

    qlonglong companyId;
    qlonglong accountId;
    qlonglong altAccountId;
    qlonglong securityAccountId;
    qlonglong payeeId;
    qlonglong categoryId;
    qlonglong groupId;

    QSignalSpy *accountUpdatedSpy;

private:
    void waitForDataLoaded(TransactionsWindow *win) {
        auto spy = QSignalSpy(win->model(), SIGNAL(dataLoaded()));
        QVERIFY(spy.isValid());
        QVERIFY(spy.wait());
        QTRY_VERIFY(win->findChild<TreeView*>()->isEnabled());
    }

    TransactionsWindow* openWindow(qlonglong accountId) {
        TransactionsWindow* window = uiContext->showTransactions(accountId);
        waitForDataLoaded(window);
        return window;
    }

    void addDetail(TreeView* treeView) {
        QTest::keyClick(treeView, Qt::Key_N, Qt::ControlModifier);
        QTest::keyClick(treeView, Qt::Key_Escape);
    }

    void enterText(QAbstractItemView *view, const char* text) {
        QSignalSpy editSpy(view->itemDelegate(), &QAbstractItemDelegate::closeEditor);
        QVERIFY(editSpy.isValid());
        QTest::keyClick(view, text[0]);
        auto editor = qobject_cast<QLineEdit*>(view->window()->focusWidget());
        QTest::keyClicks(editor, QString{text}.slice(1));
        QTest::keyClick(editor, Qt::Key_Enter);
        QVERIFY(editSpy.wait());
    }

    void selectValue(QAbstractItemView *view, const char* text) {
        QSignalSpy editSpy(view->itemDelegate(), &QAbstractItemDelegate::closeEditor);
        QVERIFY(editSpy.isValid());
        QTest::keyClick(view, text[0]);
        auto editor = qobject_cast<RelationEditor*>(view->window()->focusWidget());
        QTest::keyClicks(editor, QString{text}.slice(1));
        QTest::keySequence(editor, {Qt::Key_Enter, Qt::Key_Enter});
        QVERIFY(editSpy.wait());
    }

    void fillTransaction(TxWindowHolder& holder, const char* refNumber, const char* payee, const char* description) {
        if (holder.window->focusWidget() != holder.view) holder.focusWindow();
        QTRY_COMPARE(holder.window->focusWidget(), holder.view);
        QTest::keyClick(holder.view, Qt::Key_Tab);
        enterText(holder.view, refNumber);
        QTest::keyClick(holder.view, Qt::Key_Tab);
        selectValue(holder.view, payee);
        QTest::keyClick(holder.view, Qt::Key_Tab);
        enterText(holder.view, description);
    }

    void fillDetail(TxWindowHolder& holder, const char* category, const char* amount = nullptr, const char* group = nullptr) {
        if (holder.window->focusWidget() != holder.view) holder.focusWindow();
        QTRY_COMPARE(holder.window->focusWidget(), holder.view);
        if (!holder.view->currentIndex().parent().isValid()) QTest::keyClick(holder.view, Qt::Key_Down);
        QTest::keySequence(holder.view, {Qt::Key_Home, Qt::Key_Tab});
        if (group) selectValue(holder.view, group);
        QTest::keyClick(holder.view, Qt::Key_Tab);
        selectValue(holder.view, category);
        if (amount) {
            QTest::keySequence(holder.view, {Qt::Key_Tab, Qt::Key_Tab});
            enterText(holder.view, amount);
        }
    }

    void verifyTransaction(const Transaction* tx, QVariant refNumber, QVariant payeeId, QVariant description) {
        QCOMPARE(tx->referenceNumber, refNumber);
        QCOMPARE(tx->payeeId, payeeId);
        QCOMPARE(tx->memo, description);
    }

    void verifyDetail(qlonglong detailId, QVariant categoryId, QVariant transferAccountId, QVariant amount) {
        verifyDetail(dataStore->transactionStore->detailStore.value(detailId), categoryId, transferAccountId, amount);
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

    template<class Holder>
    void testRenameTxReference(qlonglong accountId, Holder& refHolder, int refRow, int refNameColumn, const QObject* refStore, const int TransactionTableModel::* txColumn) {
        TxWindowHolder holder(openWindow(accountId));
        QSignalSpy modelSpy(holder.model(), SIGNAL(dataChanged(const QModelIndex&,const QModelIndex&)));
        QVERIFY(modelSpy.isValid());
        refHolder.showWindow();
        refHolder.view->setCurrentIndex(refHolder.index(refRow, refNameColumn));
        QSignalSpy updateSpy(refStore, SIGNAL(valuesLoaded(QList<qlonglong>)));
        QVERIFY(updateSpy.isValid());

        enterText(refHolder.view, "new ref name");
        QTest::keyClick(refHolder.view, Qt::Key_S, Qt::ControlModifier);
        QVERIFY(updateSpy.wait());

        QVERIFY(!modelSpy.isEmpty());
        for (const auto& args : std::as_const(modelSpy)) {
            QCOMPARE(args.at(0).value<QModelIndex>().column(), holder.model()->*(txColumn));
            QCOMPARE(args.at(1).value<QModelIndex>().column(), holder.model()->*(txColumn));
        }
    }

    template<class Holder, class Store>
    void doMerge(const QVariant destinationId, const Store* store) {
        Holder refHolder(new Holder::windowType(dataStore));
        refHolder.showWindow();
        refHolder.view->setCurrentIndex(refHolder.index(1, 0));
        QSignalSpy mergeSpy(store, SIGNAL(valuesLoaded(QList<qlonglong>)));
        QVERIFY(mergeSpy.isValid());

        QTimer::singleShot(0, refHolder.window, [&]() {
            auto dialog = windowtest::findWindow<EntitySelectionDialog>();
            if (dialog) {
                dialog->setSelectedEntity(dataStore->categoryStore->value(categoryId));
                dialog->accept();
            } else QFAIL("no selection dialog");
        });
        QTest::keyClick(refHolder.view, Qt::Key_Y, Qt::ControlModifier);
        QVERIFY(mergeSpy.wait());
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        companyId = dbTestCase.addCompany(driver, COMPANY_NAME);
        accountId = dbTestCase.addAccount(driver, ACCOUNT_NAME, AccountType::bank.code, companyId)->id.value();
        altAccountId = dbTestCase.addAccount(driver, ALT_ACCOUNT_NAME, AccountType::bank.code, companyId)->id.value();
        securityAccountId = dbTestCase.addAccount(driver, SECURITY_ACCOUNT_NAME, AccountType::brokerage.code, companyId)->id.value();
        payeeId = dbTestCase.addPayee(driver, PAYEE_NAME);
        categoryId = dbTestCase.addCategory(driver, CATEGORY_NAME);
        groupId = dbTestCase.addGroup(driver, GROUP_NAME);
    }

    void init() {
        dataStore = new DataStore(&services);
        uiContext = new UiContext(dataStore);
        dbTestCase.resetDatabase(driver);
        dbTestCase.saveTransaction(driver, factory::transaction(accountId, payeeId), {factory::detail("23.45", categoryId)});
        dbTestCase.saveTransaction(driver, factory::transaction(accountId), {"34.56"});
        dbTestCase.saveTransfer(driver, altAccountId, accountId, {"78.90", "567.89"});
        accountUpdatedSpy = new QSignalSpy(dataStore->transactionStore, SIGNAL(accountUpdated(qlonglong)));
        QVERIFY(accountUpdatedSpy->isValid());
    }

    void addTransaction() {
        TxWindowHolder holder(openWindow(accountId));
        QCOMPARE(holder.window->focusWidget(), holder.view);
        auto accountTxCount = dataStore->accountStore->value(accountId)->transactions.toInt();
        auto payeeTxCount = dataStore->payeeStore->value(payeeId)->transactions.toInt();
        auto categoryDetailCount = dataStore->categoryStore->value(categoryId)->details.toInt();
        auto groupDetailCount = dataStore->groupStore->value(groupId)->details.toInt();

        fillTransaction(holder, "123", PAYEE_NAME, "description");
        fillDetail(holder, CATEGORY_NAME, "12.34", GROUP_NAME);
        QTest::keyClick(holder.view, Qt::Key_Enter);
        QVERIFY(accountUpdatedSpy->wait());
        QTRY_COMPARE(holder.window->focusWidget(), holder.view);

        QCOMPARE(dataStore->transactionStore->transactionIds(accountId).size(), INITIAL_TRANSACTION_COUNT+1);
        QCOMPARE(holder.window->model()->rowCount(), INITIAL_TRANSACTION_COUNT+2);
        auto tx = holder.window->model()->getRow(holder.index(INITIAL_TRANSACTION_COUNT, 0));
        verifyTransaction(tx, "123", payeeId, "description");
        verifyDetail(tx->detailIds.at(0).toLongLong(), categoryId, QVariant{}, "12.34");
        QVERIFY(holder.window->model()->unsavedDetailAdds().isEmpty());
        // should reset the new transaction
        verifyPendingTransaction(holder.window);
        // should update counts
        QCOMPARE(dataStore->accountStore->value(accountId)->transactions.toInt(), accountTxCount+1);
        QCOMPARE(dataStore->payeeStore->value(payeeId)->transactions.toInt(), payeeTxCount+1);
        QCOMPARE(dataStore->categoryStore->value(categoryId)->details.toInt(), categoryDetailCount+1);
        QCOMPARE(dataStore->groupStore->value(groupId)->details.toInt(), groupDetailCount+1);
    }

    void deleteTransaction_adjustsErrors() {
        TxWindowHolder holder(openWindow(accountId));
        QCOMPARE(holder.window->focusWidget(), holder.view);
        fillDetail(holder, CATEGORY_NAME);
        QCOMPARE(holder.window->model()->isValid(), false);
        holder.view->setCurrentIndex(holder.index(0, 0));
        auto accountTxCount = dataStore->accountStore->value(accountId)->transactions.toInt();
        auto payeeTxCount = dataStore->payeeStore->value(payeeId)->transactions.toInt();
        auto categoryDetailCount = dataStore->categoryStore->value(categoryId)->details.toInt();

        QTest::keySequence(holder.view, {Qt::Key_Delete, Qt::Key_Enter});
        QVERIFY(accountUpdatedSpy->wait());

        auto model = holder.window->model();
        QVERIFY(!model->transactionIsValid(model->index(model->rowCount()-1, 0)));
        QCOMPARE(model->rowCount(), INITIAL_TRANSACTION_COUNT);
        QCOMPARE(dataStore->transactionStore->transactionIds(accountId).count(), INITIAL_TRANSACTION_COUNT-1);
        QCOMPARE(dataStore->accountStore->value(accountId)->transactions.toInt(), accountTxCount-1);
        QCOMPARE(dataStore->payeeStore->value(payeeId)->transactions.toInt(), payeeTxCount-1);
        QCOMPARE(dataStore->categoryStore->value(categoryId)->details.toInt(), categoryDetailCount-1);
    }

    void addTransfer_updatesRelatedWindows() {
        TxWindowHolder holder(openWindow(accountId));
        TxWindowHolder holder2(openWindow(altAccountId));
        holder.focusWindow();
        QCOMPARE(holder2.model()->rowCount(), ALT_INITIAL_TRANSACTION_COUNT+1);
        QCOMPARE(holder.window->focusWidget(), holder.view);
        auto accountTxCount = dataStore->accountStore->value(accountId)->transactions.toInt();
        auto altAccountTxCount = dataStore->accountStore->value(altAccountId)->transactions.toInt();
        auto categoryDetailCount = dataStore->categoryStore->value(categoryId)->details.toInt();

        fillTransaction(holder, "123", PAYEE_NAME, "description");
        fillDetail(holder, ALT_ACCOUNT_NAME, "2.34");
        addDetail(holder.view);
        fillDetail(holder, CATEGORY_NAME, "321.43");
        if (holder.window->focusWidget() != holder.view) holder.focusWindow(); // FIXME why is focus moving to filter input?
        QTest::keyClick(holder.view, Qt::Key_Enter);
        QVERIFY(accountUpdatedSpy->wait());
        QTRY_COMPARE(holder.window->focusWidget(), holder.view);

        QCOMPARE(holder2.model()->rowCount(), ALT_INITIAL_TRANSACTION_COUNT+2);
        auto tx = holder2.model()->getRow(holder2.index(ALT_INITIAL_TRANSACTION_COUNT, 0));
        verifyTransaction(tx, QVariant{}, payeeId, "description");
        verifyDetail(tx->detailIds.at(0).toLongLong(), QVariant{}, accountId, "-2.34");
        verifyPendingTransaction(holder.window);
        verifyPendingTransaction(holder2.window);
        QCOMPARE(dataStore->accountStore->value(accountId)->transactions.toInt(), accountTxCount+1);
        QCOMPARE(dataStore->accountStore->value(altAccountId)->transactions.toInt(), altAccountTxCount+1);
        QCOMPARE(dataStore->categoryStore->value(categoryId)->details.toInt(), categoryDetailCount+1);
    }

    void updateDetail_updatesRelatedWindows() {
        TxWindowHolder holder(openWindow(accountId));
        TxWindowHolder holder2(openWindow(altAccountId));
        holder.focusWindow();
        auto categoryDetailCount = dataStore->categoryStore->value(categoryId)->details.toInt();

        holder.view->setCurrentIndex(holder.index(0, 0));
        QCOMPARE(holder.window->focusWidget(), holder.view);
        fillDetail(holder, ALT_ACCOUNT_NAME, "98.76");
        if (holder.window->focusWidget() != holder.view) holder.focusWindow(); // FIXME why is focus moving to filter input?
        QTest::keyClick(holder.view, Qt::Key_S, Qt::ControlModifier);
        QVERIFY(accountUpdatedSpy->wait());
        QTRY_COMPARE(holder.window->focusWidget(), holder.view);
        QTest::qWait(100);

        QCOMPARE(holder2.model()->rowCount(), ALT_INITIAL_TRANSACTION_COUNT+2);
        auto tx = holder2.model()->getRow(holder2.index(ALT_INITIAL_TRANSACTION_COUNT, 0));
        verifyTransaction(tx, QVariant{}, payeeId, QVariant{});
        verifyDetail(tx->detailIds.at(0).toLongLong(), QVariant{}, accountId, "-98.76");
        QCOMPARE(dataStore->categoryStore->value(categoryId)->details.toInt(), categoryDetailCount-1);
    }

    void deleteTransfer_updatesRelatedWindows() {
        TxWindowHolder holder(openWindow(accountId));
        TxWindowHolder holder2(openWindow(altAccountId));
        holder.focusWindow();

        holder.view->setCurrentIndex(holder.index(INITIAL_TRANSACTION_COUNT-1, 0));
        QTest::keySequence(holder.view, {Qt::Key_Delete, Qt::Key_Enter});
        QVERIFY(accountUpdatedSpy->wait());

        auto model = holder.window->model();
        QVERIFY(model->isPendingAdd(model->index(model->rowCount()-1, 0)));
        QCOMPARE(model->rowCount(), INITIAL_TRANSACTION_COUNT);
        QCOMPARE(dataStore->transactionStore->transactionIds(accountId).count(), INITIAL_TRANSACTION_COUNT-1);
        auto model2 = holder2.model();
        QCOMPARE(model2->rowCount(), ALT_INITIAL_TRANSACTION_COUNT+1);
        QCOMPARE(model2->rowCount(model2->index(0, 0)), 1);
    }

    void mergePayees_updatesTransactions() {
        auto altPayeeId = dbTestCase.addPayee(driver, ALT_PAYEE_NAME);
        auto [tx, details] = dbTestCase.saveTransaction(driver, factory::transaction(accountId, altPayeeId), {"65.78"});
        TxWindowHolder holder(openWindow(accountId));
        QSignalSpy updateSpy(dataStore->transactionStore, SIGNAL(transactionUpdated(qlonglong,int,int)));
        QVERIFY(updateSpy.isValid());

        doMerge<PayeeWindowHolder, PayeeStore>(payeeId, dataStore->payeeStore);

        if (updateSpy.isEmpty()) QVERIFY(updateSpy.wait());
        QCOMPARE(updateSpy.size(), 1);
        QCOMPARE(updateSpy.at(0).at(0), accountId);
        QVERIFY(!dataStore->payeeStore->contains(altPayeeId));
        QCOMPARE(dataStore->transactionStore->value(tx->id.value())->payeeId, payeeId);
        QCOMPARE(dataStore->transactionStore->value(tx->id.value())->version, tx->version.toLongLong()+1);
    }

    void renamePayee_updatesTransactions() {
        PayeeWindowHolder payeeHolder(new PayeesWindow(dataStore));
        testRenameTxReference(accountId, payeeHolder, 0, 0, dataStore->payeeStore, &TransactionTableModel::payeeColumn);
    }

    void renameSecurity_updatesTransactions() {
        auto securityId = dbTestCase.addSecurity(driver, "security name")->id;
        dbTestCase.saveTransaction(driver, factory::transaction(securityAccountId, QVariant{}, securityId.value()), {"123.45"});
        SecurityWindowHolder securityHolder(new SecuritiesWindow(dataStore));
        testRenameTxReference(securityAccountId, securityHolder, 0, 0, dataStore->securityStore, &TransactionTableModel::securityColumn);
    }

    void mergeCategories_updatesTransactionDetails() {
        auto altCategoryId = dbTestCase.addCategory(driver, ALT_CATEGORY_NAME);
        auto tx = factory::transaction(accountId);
        auto detail = factory::detail("65.78", altCategoryId);
        dbTestCase.saveTransaction(driver, tx, {detail});
        TxWindowHolder holder(openWindow(accountId));

        doMerge<CategoryWindowHolder, CategoryStore>(categoryId, dataStore->categoryStore);

        QVERIFY(!dataStore->categoryStore->contains(altCategoryId));
        QCOMPARE(dataStore->transactionStore->detailStore.value(detail->id.value())->categoryId, categoryId);
        QCOMPARE(dataStore->transactionStore->detailStore.value(detail->id.value())->version, detail->version.toLongLong()+1);
    }

    void renameAccount_updatesTransactionDetails() {
        auto txDetails = dbTestCase.saveTransfer(driver, accountId, altAccountId, {"65.78"});
        AccountWindowHolder accountHolder(new AccountsWindow(uiContext));
        testRenameTxReference(accountId, accountHolder, 1, 2, dataStore->accountStore, &TransactionTableModel::payeeColumn);
    }

    void renameGroup_updatesTransactionDetails() {
        auto groupId = dbTestCase.addGroup(driver, "group 1");
        auto tx = factory::transaction(accountId);
        auto detail = factory::detail("65.78", categoryId, groupId);
        dbTestCase.saveTransaction(driver, tx, {detail});
        GroupWindowHolder groupHolder(new GroupsWindow(dataStore));
        testRenameTxReference(accountId, groupHolder, 0, 0, dataStore->groupStore, &TransactionTableModel::refColumn);
    }

    void renameCompany_updatesTransactionDetails() {
        TxWindowHolder holder(openWindow(accountId));
        QSignalSpy modelSpy(holder.model(), SIGNAL(dataChanged(const QModelIndex&,const QModelIndex&)));
        QVERIFY(modelSpy.isValid());
        AccountWindowHolder accountHolder(new AccountsWindow(uiContext));
        accountHolder.showWindow();
        accountHolder.view->setCurrentIndex(accountHolder.index(1, 0));
        QSignalSpy saveSpy(&dataStore->accountStore->companyStore, SIGNAL(valuesLoaded(QList<qlonglong>)));
        QVERIFY(saveSpy.isValid());

        QTimer::singleShot(0, accountHolder.window, [&]() {
            auto dialog = windowtest::findWindow<CompaniesWindow>();
            if (dialog) {
                QVERIFY(QTest::qWaitForWindowFocused(dialog));
                auto table = dialog->findChild<QTableView *>();
                enterText(table, "new company name");
                QTest::keyClick(table, Qt::Key_S, Qt::ControlModifier);
            } else QFAIL("no company dialog");
        });
        QTest::keyClick(accountHolder.view, Qt::Key_C, Qt::AltModifier);
        QVERIFY(saveSpy.wait());

        QCOMPARE(modelSpy.size(), 1);
        QCOMPARE(modelSpy.at(0).at(0).value<QModelIndex>().column(), holder.model()->payeeColumn);
        QCOMPARE(modelSpy.at(0).at(1).value<QModelIndex>().column(), holder.model()->payeeColumn);
    }

    void cleanup() {
        delete accountUpdatedSpy;
        accountUpdatedSpy = nullptr;
        dbTestCase.cleanup();
        delete uiContext;
        uiContext = nullptr;
        delete dataStore;
        dataStore = nullptr;
    }
};

QTEST_MAIN(TestTransactionsWindow)
#include "test_transactionswindow.moc"