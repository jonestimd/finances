#ifndef DIALOG_H
#define DIALOG_H

#include <QMessageBox>

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

void showError(QWidget *parent, const QString message);

#endif // DIALOG_H
