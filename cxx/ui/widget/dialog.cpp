#include "dialog.h"

#include <QTimer>

void showError(QWidget *parent, const QString message) {
    QTimer::singleShot(0, parent, [parent, message] {
        QMessageBox::critical(parent, parent->tr("Error"), message, QMessageBox::Ok);
    });
}
