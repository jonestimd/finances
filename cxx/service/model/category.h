#ifndef CATEGORY_H
#define CATEGORY_H

#include "basedomain.h"
#include "amounttype.h"
#include <QSqlRecord>
#include <QVariant>

class Category : public TransactionType {
public:
    QVariant amountType{DEBIT_DEPOSIT};
    QVariant description;
    QVariant income{false};
    QVariant security{false};
    optional_id parentId{};
    QList<domain_id> childIds{};
    mutable int details{0};

    Category();
    Category(const QSqlRecord &record);

    bool deletable() const;
};

#endif // CATEGORY_H
