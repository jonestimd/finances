#ifndef REQUIRED_H
#define REQUIRED_H

#include "validatorfactory.h"
#include <QValidator>

class RequiredValidatorFactory : public ValidatorFactory {
public:
    RequiredValidatorFactory();

    const QString isValid(const QModelIndex &index, QString &value) const override;
    const QString isValid(const QModelIndex &index, QString &value, GetTitle getTitle) const;
};

Q_GLOBAL_STATIC(RequiredValidatorFactory, requiredValidatorFactory)

#endif // REQUIRED_H
