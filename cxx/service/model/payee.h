#ifndef PAYEE_H
#define PAYEE_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class Payee : public BaseDomain {
public:
    QVariant name;
    QVariant transactions;

    Payee();
    Payee(QSqlRecord record);

    bool deletable() const;
};

#endif // PAYEE_H
