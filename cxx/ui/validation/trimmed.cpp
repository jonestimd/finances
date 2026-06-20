#include "trimmed.h"
#include <QModelIndex>

TrimmedValidatorFactory::TrimmedValidatorFactory() : ValidatorFactory{false, true} {}

const QString TrimmedValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    if (value.length() > 0) {
        if ((value.front().isSpace())) return tr("%1 contains leading whitespace").arg(columnHeader(index));
        if ((value.back().isSpace())) return tr("%1 contains trailing whitespace").arg(columnHeader(index));
    }
    return nullptr;
}

void TrimmedValidatorFactory::fixup(QString &text) const {
    text = text.trimmed();
}
