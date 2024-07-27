#include "unique.h"

UniqueValidatorFactory::UniqueValidatorFactory() : ValidatorFactory(true) {}

const QString UniqueValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    QList<QVariant> values;
    auto model = index.model();
    for (int r = 0; r < model->rowCount(); r++) {
        if (r == index.row()) continue;
        auto other = model->data(model->index(r, index.column()));
        values.append(other.toString().toLower());
    }

    return values.contains(value.trimmed().toLower()) ? formatMessage(tr("%1 must be unique"), index) : nullptr;
}

#include "unique.moc"
