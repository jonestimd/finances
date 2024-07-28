#ifndef COMPANY_H
#define COMPANY_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class Company : public NamedEntity {
public:
    QVariant name;
    QVariant accounts;

    Company();
    Company(QSqlRecord record);
    Company(const QString &name);

    QString displayName() const override;
    bool deletable() const;
};

#endif // COMPANY_H
