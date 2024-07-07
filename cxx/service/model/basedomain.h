#ifndef BASEDOMAIN_H
#define BASEDOMAIN_H

#include <QSqlRecord>
#include <QVariant>

class BaseDomain {
public:
    QVariant id;
    QVariant version;
    QVariant changeUser;
    QVariant changeDate;

    BaseDomain();
    BaseDomain(QSqlRecord record);
};

#endif // BASEDOMAIN_H
