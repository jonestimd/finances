#include "unique.h"

class UniqueValidator : public ValidationStatus
{
    const QString message;
    QList<QVariant> values;
public:
    UniqueValidator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar)
        : ValidationStatus{index, parent, statusBar}
        , message{formatMessage("%1 must be unique", index)}
    {
        auto model = index.model();
        for (int r = 0; r < model->rowCount()-1; ++r) {
            if (r == index.row()) continue;
            auto other = model->data(model->index(r, index.column()));
            values.append(other.toString().toLower());
        }
    }

    const QString isValid(QString &value) const override {
        return values.contains(value.trimmed().toLower()) ? message : nullptr;
    }
};

UniqueValidatorFactory::UniqueValidatorFactory() {}

const ValidationStatus *UniqueValidatorFactory::validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar) const {
    return new UniqueValidator(index, parent, statusBar);
}
