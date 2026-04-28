#include <QTest>
#include "dbtestcase.h"
#include "service/categoryservice.h"

class TestCategoryService : public QObject {
    Q_OBJECT
    DbTestCase dbTestCase{};

private slots:
    void initTestCase_data() {
        dbTestCase.createDatabases();
        QTest::addColumn<QString>("driver");
        QTest::addColumn<CategoryService*>("service");
        for (auto &driver : dbTestCase.connectionPoolNames()) {
            auto service = new CategoryService{dbTestCase.connectionPool(driver)};
            QTest::newRow(driver.toLocal8Bit()) << driver << service;
        }
    }

    void setParent_returnsOldParent() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(CategoryService*, service);
        auto parent = dbTestCase.addCategory(driver, "parent");
        auto child = dbTestCase.addCategory(driver, "child", parent);

        auto result = service->setParent(child, QVariant{}, TEST_USER);

        QCOMPARE(result.size(), 2);
        QCOMPARE(result.value(parent->id.toLongLong())->name, parent->name);
        QCOMPARE(result.value(parent->id.toLongLong())->childIds, {});
        QCOMPARE(result.value(child->id.toLongLong())->parentId, QVariant{});
    }

    void setParent_returnsOldAndNewParents() {
        QFETCH_GLOBAL(QString, driver);
        QFETCH_GLOBAL(CategoryService*, service);
        auto parent = dbTestCase.addCategory(driver, "parent");
        auto child = dbTestCase.addCategory(driver, "child", parent);
        auto newParent = dbTestCase.addCategory(driver, "new parent");

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
        auto parent = dbTestCase.addCategory(driver, "parent");
        auto child = dbTestCase.addCategory(driver, "child", parent);
        auto toMerge = dbTestCase.addCategory(driver, "other parent");
        auto otherChild = dbTestCase.addCategory(driver, "other child", toMerge);

        auto result = service->merge(toMerge, parent->id, TEST_USER);

        QCOMPARE(result.size(), 2);
        QCOMPARE(result.value(parent->id.toLongLong())->name, parent->name);
        QCOMPARE(result.value(otherChild->id.toLongLong())->name, otherChild->name);
        QCOMPARE(result.value(otherChild->id.toLongLong())->parentId, parent->id);
        QVariantList childIds{child->id, otherChild->id};
        QCOMPARE(result.value(parent->id.toLongLong())->childIds, childIds);
    }
};

QTEST_GUILESS_MAIN(TestCategoryService)
#include "test_categoryservice.moc"
