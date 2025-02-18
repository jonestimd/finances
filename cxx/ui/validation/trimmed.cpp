#include "trimmed.h"
#include <QModelIndex>

TrimmedValidatorFactory::TrimmedValidatorFactory() : ValidatorFactory{false, true} {}

const QString TrimmedValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    return isValid(index, value, columnHeader);
}

const QString TrimmedValidatorFactory::isValid(const QModelIndex &index, QString &value, GetTitle getTitle) const {
    if (value.length() > 0) {
        if ((value.front().isSpace())) return tr("%1 contains leading whitespace").arg(getTitle(index));
        if ((value.back().isSpace())) return tr("%1 contains trailing whitespace").arg(getTitle(index));
    }
    return nullptr;
}

void TrimmedValidatorFactory::fixup(QString &text) const {
    text = text.trimmed();
}
