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

void doInBackground(QWidget *source, Runnable task, OnComplete onComplete)
{
    QThreadPool::globalInstance()->start([=]() {
        auto result = handleError(source, task);
        if (onComplete) onComplete(result);
    });
}
