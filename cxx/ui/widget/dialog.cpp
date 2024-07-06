#include "dialog.h"
#include <QTimer>

namespace dialog {
    void showError(QWidget *parent, const QString message) {
        QTimer::singleShot(0, parent, [parent, message] {
            QMessageBox::critical(parent, parent->tr("Error"), message, QMessageBox::Ok);
        });
    }

    bool confirmDelete(QWidget *parent, const char *title, const char *message, QStringList items) {
        return items.empty() || QMessageBox::Yes == QMessageBox::warning(parent,
                parent->tr(title), parent->tr(message).arg(items.join(DIALOG_ITEM_SEPARATOR)),
                QMessageBox::Yes | QMessageBox::No, QMessageBox::Yes);
    }
}
