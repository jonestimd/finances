#ifndef COMPOSITE_H
#define COMPOSITE_H

#include "factory.h"
#include "required.h"
#include "unique.h"

class CompositeValidatorFactory : public ValidatorFactory {
    QList<ValidatorFactory*> factories;
public:
    CompositeValidatorFactory(QList<ValidatorFactory*> factories);

    const QValidator *validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr) override;
};

Q_GLOBAL_STATIC(CompositeValidatorFactory, requiredUniqueFactory,
                QList<ValidatorFactory*>{requiredValidatorFactory, uniqueValidatorFactory})

#endif // COMPOSITE_H
