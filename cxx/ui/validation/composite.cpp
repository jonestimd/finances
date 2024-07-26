#include "composite.h"

bool CompositeValidatorFactory::anyMultiRow(QList<ValidatorFactory*> factories) {
    for (auto factory : factories) {
        if (factory->multiRow) return true;
    }
    return false;
}

CompositeValidatorFactory::CompositeValidatorFactory(QList<ValidatorFactory*> factories)
    : ValidatorFactory(anyMultiRow(factories))
    , factories{factories} {};

const QString CompositeValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    for (auto factory: factories) {
        auto message = factory->isValid(index, value);
        if (message != nullptr) return message;
    }
    return nullptr;
}

void CompositeValidatorFactory::fixup(QString &text) const {
    for (auto factory: factories) {
        factory->fixup(text);
    }
}
