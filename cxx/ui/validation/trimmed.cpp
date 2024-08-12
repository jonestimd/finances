#include "trimmed.h"
#include <QModelIndex>

TrimmedValidatorFactory::TrimmedValidatorFactory() {}

const QString TrimmedValidatorFactory::isValid(const QModelIndex &index, QString &value) const {
    if (value.length() > 0) {
        if ((value.front().isSpace())) return formatMessage(tr("%1 contains leading whitespace"), index);
        if ((value.back().isSpace())) return formatMessage(tr("%1 contains trailing whitespace"), index);
    }
    return nullptr;
}

void TrimmedValidatorFactory::fixup(QString &text) const {
    text = text.trimmed();
}
