#ifndef TRANSACTIONGROUP_H
#define TRANSACTIONGROUP_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class TransactionGroup : public NamedEntity {
public:
    QVariant name;
    QVariant description;
    QVariant transactions{0};
    QVariant details{0};

    TransactionGroup();
    TransactionGroup(const QSqlRecord &record);
    TransactionGroup(const QString &name);

    bool deletable() const;
};

#endif // TRANSACTIOGROUP_H
