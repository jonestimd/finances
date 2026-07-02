#include "background.h"
#include "../widget/dialog.h"
#include <QThreadPool>

bool handleError(QWidget *source, Runnable task) {
    try {
        task();
        return true;
    } catch(const QString error) {
        dialog::showError(source, error);
        QMetaObject::invokeMethod(source, "enableUi");
    }
    return false;
}

// FIXME add Runnable to update UI in UI thread or add mutex on UI data
void doInBackground(QWidget *source, Runnable task, Runnable onError) {
    QThreadPool::globalInstance()->start([=]() {
        auto success = handleError(source, task);
        if (!success && onError) onError();
    });
}
