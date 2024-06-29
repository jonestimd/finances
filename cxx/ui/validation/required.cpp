#include "required.h"
#include "status.h"
#include <QModelIndex>

class RequiredValidator : public ValidationStatus
{
public:
    RequiredValidator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar)
        : ValidationStatus(index, parent, statusBar, "%1 is required") {}

    State validate(QString &value, int &pos) const override {
        return showStatus(value.trimmed().isEmpty());
    }
};

RequiredValidatorFactory::RequiredValidatorFactory() {}

const QValidator *RequiredValidatorFactory::validator(const QModelIndex &index, QObject *parent, QStatusBar *statusBar) {
    return new RequiredValidator(index, parent, statusBar);
}
