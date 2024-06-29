#include "status.h"
#include <QModelIndex>

ValidationStatus::ValidationStatus(const QModelIndex &index, QObject *parent, QStatusBar *statusBar, const char *format)
    : QValidator{parent}, statusBar{statusBar}
{
    if (statusBar) {
        auto model = index.model();
        auto title = model->headerData(index.column(), Qt::Horizontal);
        message = tr(format).arg(title.toString());
    }
}

QValidator::State ValidationStatus::showStatus(bool invalid) const {
    if (invalid) {
        if (statusBar) statusBar->showMessage(message);
        return QValidator::State::Intermediate;
    }
    if (statusBar) statusBar->clearMessage();
    return QValidator::State::Acceptable;
}
