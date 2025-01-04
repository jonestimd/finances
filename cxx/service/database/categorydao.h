#ifndef CATEGORY_DAO_H
#define CATEGORY_DAO_H

#include "entitydao.h"
#include "../model/category.h"
#include <QtSql/QSqlDatabase>

class CategoryDao : public QObject, public EntityDao<Category> {
    Q_OBJECT
public:
    CategoryDao();

    QList<const Category*> setParent(QSqlDatabase &db, const Category* category, const QVariant parentId, const QString user);

protected:
    virtual void bindUpdateValues(QSqlQuery &query, Category *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, Category *entity) override;
};

Q_GLOBAL_STATIC(CategoryDao, categoryDao)

#endif // CATEGORY_DAO_H
