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
    std::optional<qlonglong> parentId{};
    QList<qlonglong> childIds{};
    mutable QVariant details{0};

    Category();
    Category(const QSqlRecord &record);

    bool deletable() const;
};

#endif // CATEGORY_H
