#ifndef COMPOSITE_H
#define COMPOSITE_H

#include "factory.h"
#include "required.h"
#include "trimmed.h"
#include "unique.h"

class CompositeValidatorFactory : public ValidatorFactory {
    QList<ValidatorFactory*> factories;
public:
    CompositeValidatorFactory(QList<ValidatorFactory*> factories);

    const ValidationStatus *validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar = nullptr) const override;
};

Q_GLOBAL_STATIC(CompositeValidatorFactory, requiredUniqueFactory,
                QList<ValidatorFactory*>{trimmedValidatorFactory, requiredValidatorFactory, uniqueValidatorFactory})

#endif // COMPOSITE_H
