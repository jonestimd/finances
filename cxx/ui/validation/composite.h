#ifndef COMPOSITE_H
#define COMPOSITE_H

#include "factory.h"
#include "required.h"
#include "trimmed.h"
#include "unique.h"

class CompositeValidatorFactory : public ValidatorFactory {
    static bool anyMultiRow(QList<ValidatorFactory*> factories);

    QList<ValidatorFactory*> factories;
public:
    CompositeValidatorFactory(QList<ValidatorFactory*> factories);

    const QString isValid(const QModelIndex &index, QString &value) const override;

    void fixup(QString &text) const override;
};

Q_GLOBAL_STATIC(CompositeValidatorFactory, requiredUniqueFactory,
                QList<ValidatorFactory*>{trimmedValidatorFactory, requiredValidatorFactory, uniqueValidatorFactory})

#endif // COMPOSITE_H
