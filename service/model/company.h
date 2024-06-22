#ifndef COMPANY_H
#define COMPANY_H

#include "basedomain.h"
#include <QSqlRecord>
#include <QVariant>

class Company : public BaseDomain {
public:
    QVariant name;

    Company();
    Company(QSqlRecord record);
};

#endif // COMPANY_H
