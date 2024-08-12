#ifndef PAYEE_H
#define PAYEE_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class Payee : public NamedEntity {
public:
    QVariant name;
    QVariant transactions;

    Payee();
    Payee(QSqlRecord record);

    bool deletable() const;

    QString displayName() const override;
};

#endif // PAYEE_H
