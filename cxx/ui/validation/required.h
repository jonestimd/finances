#ifndef REQUIRED_H
#define REQUIRED_H

#include "factory.h"
#include <QValidator>

class RequiredValidatorFactory : public ValidatorFactory {
public:
    RequiredValidatorFactory();

    const QValidator *validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr) override;
};

Q_GLOBAL_STATIC(RequiredValidatorFactory, requiredValidatorFactory)

#endif // REQUIRED_H
