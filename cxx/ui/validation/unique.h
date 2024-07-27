#ifndef UNIQUE_H
#define UNIQUE_H

#include "validatorfactory.h"
#include <QModelIndex>
#include <QValidator>

class UniqueValidatorFactory : public ValidatorFactory {
    Q_OBJECT
public:
    UniqueValidatorFactory();

    const QString isValid(const QModelIndex &index, QString &value) const override;
};

Q_GLOBAL_STATIC(UniqueValidatorFactory, uniqueValidatorFactory)

#endif // UNIQUE_H
