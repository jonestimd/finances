#ifndef PAYEE_H
#define PAYEE_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class Payee : public NamedEntity {
public:
    QVariant name;
    QVariant transactions{0};

    Payee();
    Payee(QSqlRecord record);
    Payee(const QString &name);

    bool deletable() const;
};

#endif // PAYEE_H
