#include "dialog.h"
#include <QTimer>

namespace dialog {
    void showError(QWidget *parent, const QString message) {
        QTimer::singleShot(0, parent, [parent, message] {
            QMessageBox::critical(parent, QMessageBox::tr("Error"), message, QMessageBox::Ok);
        });
    }

    bool confirmDelete(QWidget *parent, const QString title, const QString message, QStringList items) {
        return items.empty() || QMessageBox::Yes == QMessageBox::warning(parent,
                title, message.arg(items.join(DIALOG_ITEM_SEPARATOR)),
                QMessageBox::Yes | QMessageBox::No, QMessageBox::Yes);
    }
}
