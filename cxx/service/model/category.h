#ifndef CATEGORY_H
#define CATEGORY_H

#include "basedomain.h"
#include "amounttype.h"
#include <QSqlRecord>
#include <QVariant>

class Category : public TransactionType {
public:
    const AmountType* amountType{&AmountType::debitDeposit};
    QString description;
    bool income{false};
    bool security{false};
    optional_id parentId{};
    QList<domain_id> childIds{};
    mutable int details{0};

    Category();
    Category(const QSqlRecord &record);

    bool deletable() const;
};

#endif // CATEGORY_H
