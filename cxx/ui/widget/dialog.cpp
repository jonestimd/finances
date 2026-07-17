#include "dialog.h"
#include <QTimer>

namespace dialog {
    static QMultiHash<const QWidget*, const QString> windowErrors{};

    void showError(QWidget *parent, const QString message) {
        if (!windowErrors.contains(parent, message)) {
            windowErrors.insert(parent, message);
            QTimer::singleShot(0, parent, [parent, message] {
                QMessageBox::critical(parent, QMessageBox::tr("Error"), message, QMessageBox::Ok);
                windowErrors.remove(parent, message);
            });
        }
    }

    bool confirmReplaceFile(QWidget *parent, const QString name) {
        QMessageBox dialog{parent};
        dialog.setWindowTitle(QObject::tr("Confirm Replace File"));
        dialog.setIcon(QMessageBox::Question);
        dialog.setText(QObject::tr("A file named \"%1\" already exists.\nDo you want to replace it?").arg(name));
        dialog.addButton(QObject::tr("&Replace"), QMessageBox::YesRole);
        dialog.addButton(QObject::tr("&Cancel"), QMessageBox::NoRole);
        if (dialog.exec() == -1) return false;
        return dialog.buttonRole(dialog.clickedButton()) == QMessageBox::YesRole;
    }

    bool confirmDelete(QWidget *parent, const QString title, const QString message, QStringList items) {
        return items.empty() || QMessageBox::Yes == QMessageBox::warning(parent,
                title, message.arg(items.join(DIALOG_ITEM_SEPARATOR)),
                QMessageBox::Yes | QMessageBox::No, QMessageBox::Yes);
    }
}
