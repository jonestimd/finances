#include "status.h"
#include <QModelIndex>

ValidationStatus::ValidationStatus(const QModelIndex &index, QObject *parent, QStatusBar *statusBar)
    : QValidator{parent}
    , statusBar{statusBar}
{}

QValidator::State ValidationStatus::showStatus(const QString message) const {
    if (!message.isNull()) {
        if (statusBar) statusBar->showMessage(message);
        return QValidator::State::Intermediate;
    }
    if (statusBar) statusBar->clearMessage();
    return QValidator::State::Acceptable;
}

QValidator::State ValidationStatus::validate(QString &value, int &pos) const {
    return showStatus(isValid(value));
}

QString ValidationStatus::formatMessage(const char *format, const QModelIndex &index) {
    return tr(format).arg(index.model()->headerData(index.column(), Qt::Horizontal).toString());
}
