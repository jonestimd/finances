#include "required.h"
#include <QModelIndex>

class RequiredValidator : public ValidationStatus
{
    const QString message;
public:
    RequiredValidator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar)
        : ValidationStatus(index, parent, statusBar)
        , message{formatMessage("%1 is required", index)}
    {}

    const QString isValid(QString &value) const override {
        return value.trimmed().isEmpty() ? message : nullptr;
    }
};

RequiredValidatorFactory::RequiredValidatorFactory() {}

const ValidationStatus *RequiredValidatorFactory::validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar) const {
    return new RequiredValidator(index, parent, statusBar);
}
