#include "composite.h"

class CompositeValidator : public QValidator
{
    QList<const QValidator*> validators;
public:
    CompositeValidator(QList<const QValidator*> validators, QObject *parent)
        : QValidator{parent}, validators{validators} {}

    State validate(QString &value, int &pos) const override {
        for (auto validator : validators) {
            auto state = validator->validate(value, pos);
            if (state != Acceptable) return state;
        }
        return Acceptable;
    }
};

CompositeValidatorFactory::CompositeValidatorFactory(QList<ValidatorFactory*> factories)
    : factories{factories} {};

const QValidator *CompositeValidatorFactory::validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar) {
    QList<const QValidator*> validators;
    for (auto factory : factories) {
        validators.append(factory->validator(index, parent, statusBar));
    }
    return new CompositeValidator(validators, parent);
}
