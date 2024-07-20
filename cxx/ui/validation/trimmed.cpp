#include "trimmed.h"
#include <QModelIndex>

class TrimmedValidator : public ValidationStatus
{
    const QString message;
public:
    TrimmedValidator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar)
        : ValidationStatus(index, parent, statusBar)
        , message{formatMessage("%1 contains leading/trailing whitespace", index)}
    {}

    const QString isValid(QString &value) const override {
        return value != value.trimmed() ? message : nullptr;
    }

    void fixup(QString &text) const override {
        text = text.trimmed();
    }
};

TrimmedValidatorFactory::TrimmedValidatorFactory() {}

const ValidationStatus *TrimmedValidatorFactory::validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar) const {
    return new TrimmedValidator(index, parent, statusBar);
}
