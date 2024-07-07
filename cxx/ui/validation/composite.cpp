#include "composite.h"

class CompositeValidator : public ValidationStatus
{
    QList<const ValidationStatus*> validators;
public:
    CompositeValidator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar, QList<const ValidationStatus*> validators)
        : ValidationStatus{index, parent, statusBar}, validators{validators} {}

    const QString isValid(QString &value) const override {
        for (auto validator : validators) {
            auto message = validator->isValid(value);
            if (message != nullptr) return message;
        }
        return nullptr;
    }
};

CompositeValidatorFactory::CompositeValidatorFactory(QList<ValidatorFactory*> factories)
    : factories{factories} {};

const ValidationStatus *CompositeValidatorFactory::validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar) const {
    QList<const ValidationStatus*> validators;
    for (auto factory : factories) {
        validators.append(factory->validator(index, parent));
    }
    return new CompositeValidator(index, parent, statusBar, validators);
}
