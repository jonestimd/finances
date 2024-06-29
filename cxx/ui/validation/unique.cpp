#include "unique.h"
#include "status.h"

class UniqueValidator : public ValidationStatus
{
    QList<QVariant> values;
public:
    UniqueValidator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar)
        : ValidationStatus{index, parent, statusBar, "%1 must be unique"}
    {
        auto model = index.model();
        for (int r = 0; r < model->rowCount()-1; ++r) {
            if (r == index.row()) continue;
            auto other = model->data(model->index(r, index.column()));
            values.append(other.toString().toLower());
        }
    }

    State validate(QString &value, int &pos) const override {
        return showStatus(values.contains(value.trimmed().toLower()));
    }
};

UniqueValidatorFactory::UniqueValidatorFactory() {}

const QValidator *UniqueValidatorFactory::validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar) {
    return new UniqueValidator(index, parent, statusBar);
}
