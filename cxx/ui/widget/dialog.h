#ifndef DIALOG_H
#define DIALOG_H

#include <QMessageBox>

#define DIALOG_ITEM_SEPARATOR "\n \u2022 "

namespace dialog {
    template<typename T>
    bool confirmClose(QWidget *parent, T *model) {
        if (model->hasUnsavedChanges()) {
            auto confirm = QMessageBox::warning(parent, parent->tr("Unsaved changes"),
                                                parent->tr("Discard unsaved chnages?"),
                                                QMessageBox::Discard | QMessageBox::Cancel);
            if (confirm != QMessageBox::Discard) return false;
            model->clearChanges();
        }
        return true;
    }

    bool confirmDelete(QWidget *parent, const char *title, const char *message, QStringList items);

    void showError(QWidget *parent, const QString message);
}
#endif // DIALOG_H
