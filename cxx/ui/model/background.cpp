#include "background.h"
#include "../widget/dialog.h"

bool handleError(QWidget *source, Runnable task) {
    try {
        task();
        return true;
    } catch(const QString error) {
        dialog::showError(source, error);
    }
    return false;
}

void doInBackground(QWidget *source, Runnable task, Runnable onError)
{
    QThreadPool::globalInstance()->start([=]() {
        auto success = handleError(source, task);
        if (!success && onError) onError();
    });
}
