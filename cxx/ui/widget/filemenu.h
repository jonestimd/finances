#ifndef FILEMENU_H
#define FILEMENU_H

#include "ui/store/datastore.h"
#include "ui/widget/appwindow.h"
#include <QMenu>

class UiContext;

class FileMenu : public QMenu {
    Q_OBJECT
    QMenu recentsMenu{tr("Recent &Files")};
    const QString connectionName;
    StatusMessageStore* const messageStore;

public:
    FileMenu(AppWindow* window, UiContext* context);

    Q_SLOT void updateRecentsMenu();

private:
    void handleOpenResult(DataStore* dataStore, const QString& error);

    inline AppWindow* appWindow() const {
        return qobject_cast<AppWindow*>(parent());
    }

private:
    Q_SLOT void openConnection();
};

#endif // FILEMENU_H