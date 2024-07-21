#ifndef DIALOG_H
#define DIALOG_H

#include <QMessageBox>

#define DIALOG_ITEM_SEPARATOR "\n \u2022 "

namespace dialog {
    template<typename T>
    bool confirmDiscardChanges(QWidget *parent, T *model) {
        if (model->hasUnsavedChanges()) {
            auto confirm = QMessageBox::warning(parent, QObject::tr("Unsaved changes"),
                                                QObject::tr("Discard unsaved changes?"),
                                                QMessageBox::Yes| QMessageBox::No);
            if (confirm != QMessageBox::Yes) return false;
            model->clearChanges();
        }
        return true;
    }

    bool confirmDelete(QWidget *parent, const char *title, const char *message, QStringList items);

    void showError(QWidget *parent, const QString message);
}
#endif // DIALOG_H
