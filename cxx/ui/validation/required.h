#ifndef REQUIRED_H
#define REQUIRED_H

#include "validatorfactory.h"
#include <QValidator>

class RequiredValidatorFactory : public ValidatorFactory {
    Q_OBJECT
public:
    RequiredValidatorFactory();

    const QString isValid(const QModelIndex &index, QString &value) const override;
};

Q_GLOBAL_STATIC(RequiredValidatorFactory, requiredValidatorFactory)

#endif // REQUIRED_H
