#ifndef CATEGORY_DAO_H
#define CATEGORY_DAO_H

#include "entitydao.h"
#include "../model/category.h"
#include <QtSql/QSqlDatabase>

class CategoryDao : public NamedEntityDao<Category> {
    const char *createTableSql;

public:
    CategoryDao(const QString &dbType);

    void createTable(const QSqlDatabase &db) const;

    QHash<qlonglong, const Category*> setParent(QSqlDatabase &db, const Category* category, const QVariant parentId, const QString user);
    void moveChildren(QSqlDatabase &db, const Category* category, const QVariant destinationId, const QString user) const;

protected:
    virtual void bindUpdateValues(QSqlQuery &query, Category *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, Category *entity) override;
};

#endif // CATEGORY_DAO_H
