#include <QTest>
#include "dbtestcase.h"
#include "service/categoryservice.h"

class TestCategoryService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

    QList<const Category*> categories{};

    Category *addCategory(const QString &driver, const QString &name, Category *parent = nullptr) {
        Connection conn(dbTestCase.connectionPool(driver));
        auto testName = QTest::currentTestFunction();
        Category *category = new Category;
        category->name = QString("%0:%1").arg(testName, name);
        category->parentId = parent ? parent->id : QVariant{};
        dbTestCase.categoryDao(driver).add(conn.db, {category}, TEST_USER);
        if (parent) parent->childIds.append(category->id);
        this->categories.append(category);
        return category;
    }

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<CategoryService*>("service");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto &dao = dbTestCase.categoryDao(driver);
            auto &detailDao = dbTestCase.detailDao(driver);
            auto service = new CategoryService{dbTestCase.connectionPool(driver), dao, detailDao};
            QTest::newRow(driver.toLocal8Bit()) << driver << service;
        }
    }

    void setParent_returnsOldParent() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(CategoryService*, service);
        auto parent = addCategory(driver, "parent");
        auto child = addCategory(driver, "child", parent);

        auto result = service->setParent(child, QVariant{}, TEST_USER);

        QCOMPARE(result.size(), 2);
        QCOMPARE(result.value(parent->id.toLongLong())->name, parent->name);
        QCOMPARE(result.value(parent->id.toLongLong())->childIds, {});
        QCOMPARE(result.value(child->id.toLongLong())->parentId, QVariant{});
    }

    void setParent_returnsOldAndNewParents() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(CategoryService*, service);
        auto parent = addCategory(driver, "parent");
        auto child = addCategory(driver, "child", parent);
        auto newParent = addCategory(driver, "new parent");

        auto result = service->setParent(child, newParent->id, TEST_USER);

        QCOMPARE(result.size(), 3);
        QCOMPARE(result.value(parent->id.toLongLong())->name, parent->name);
        QCOMPARE(result.value(parent->id.toLongLong())->childIds, {});
        QCOMPARE(result.value(newParent->id.toLongLong())->name, newParent->name);
        QCOMPARE(result.value(newParent->id.toLongLong())->childIds, {child->id});
        QCOMPARE(result.value(child->id.toLongLong())->parentId, newParent->id);
    }

    void merge_updatesChildren() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(CategoryService*, service);
        auto parent = addCategory(driver, "parent");
        auto child = addCategory(driver, "child", parent);
        auto toMerge = addCategory(driver, "other parent");
        auto otherChild = addCategory(driver, "other child", toMerge);

        auto result = service->merge(toMerge, parent->id, TEST_USER);

        QCOMPARE(result.size(), 2);
        QCOMPARE(result.value(parent->id.toLongLong())->name, parent->name);
        QCOMPARE(result.value(otherChild->id.toLongLong())->name, otherChild->name);
        QCOMPARE(result.value(otherChild->id.toLongLong())->parentId, parent->id);
        QVariantList childIds{child->id, otherChild->id};
        QCOMPARE(result.value(parent->id.toLongLong())->childIds, childIds);
    }

    void cleanup() {
        for (auto category : std::as_const(categories)) delete category;
        categories.clear();
    }
};

QTEST_GUILESS_MAIN(TestCategoryService)
#include "test_categoryservice.moc"
