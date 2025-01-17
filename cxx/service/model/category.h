#ifndef CATEGORY_H
#define CATEGORY_H

#include "basedomain.h"
#include "amounttype.h"
#include <QSqlRecord>
#include <QVariant>

class Category : public NamedEntity {
public:
    QVariant amountType{DEBIT_DEPOSIT};
    QVariant description;
    QVariant income{false};
    QVariant security{false};
    QVariant parentId;
    QList<QVariant> childIds{};
    QVariant transactions{0};
    QVariant details{0};

    Category();
    Category(const QSqlRecord &record);
    Category(const QString &name);

    bool deletable() const;
};

#endif // CATEGORY_H
