#ifndef CATEGORY_H
#define CATEGORY_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class Category : public NamedEntity {
public:
    QVariant name;
    QVariant amountType;
    QVariant description;
    QVariant income;
    QVariant security;
    QVariant parentId;
    QVariant transactions;

    Category();
    Category(QSqlRecord record);

    bool deletable() const;

    QString displayName() const override;

    static QHash<qlonglong, const Category*> categories;
};

#endif // CATEGORY_H
