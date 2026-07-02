#ifndef COMPANY_H
#define COMPANY_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class Company : public NamedEntity {
public:
    int accounts{0};

    Company();
    Company(const QSqlRecord &record);
    Company(const QString &name);

    bool deletable() const;
};

#endif // COMPANY_H
