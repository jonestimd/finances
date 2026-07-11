#include "connectiondialog.h"
#include "ui/uicontext.h"
#include "filemenu.h"
#include <QComboBox>
#include <QDialogButtonBox>
#include <QFileDialog>
#include <QFormLayout>
#include <QMessageBox>
#include <QPushButton>
#include <QToolButton>

#define CONNECTION_PROP "connectionName"

namespace filemenu {
    static const QHash<const QString, const char*> typeMap{
        {MYSQL_DRIVER, "MySQL"},
        {PG_DRIVER, "Postgres"},
    };

    using namespace finances;
    using Mode = ConnectionDialog::Mode;

    class FileAction : public QAction { // TODO add dialogAction() factory to finances.cpp
    public:
        FileAction(const QString& name, const QKeySequence& shortcut, QWidget* window, Mode mode = Mode::Open)
            : QAction(window)
        {
            initAction(this, FontIcon::None, name, shortcut);
            connect(this, &FileAction::triggered, [=]() {
                ConnectionDialog dialog{window, mode};
                dialog.exec();
            });
        };
    };
}

using namespace filemenu;

FileMenu::FileMenu(AppWindow* window, const QString &connectionName)
    : QMenu(tr("&File"), window)
    , connectionName{connectionName}
{
    addAction(new FileAction(tr("&New Database..."), QKeyCombination{}, window, Mode::Create));
    addAction(new FileAction(tr("&Open Database..."), QKeyCombination{Qt::ControlModifier, Qt::Key_O}, window));
    addMenu(&recentsMenu);
    connect(qApp, SIGNAL(recentAdded()), this, SLOT(updateRecentsMenu()));
    updateRecentsMenu();
}

void FileMenu::updateRecentsMenu() {
    recentsMenu.clear();
    const auto& recentNames = App::getRecentNames();
    for (auto i = recentNames.rbegin(); i != recentNames.rend(); i++) {
        if (*i != connectionName) {
            auto parts = ConnectionSettings::parseConfigName(*i);
            QString text =  parts[0] == SQLITE_DRIVER
                    ? parts[3]
                    : QStringList{typeMap.value(parts[0]), parts[1], parts[3]}.join(':');
            auto action = recentsMenu.addAction(text);
            action->setProperty(CONNECTION_PROP, *i);
            connect(action, SIGNAL(triggered(bool)), this, SLOT(openConnection()));
        }
    }
}

void FileMenu::openConnection() {
    auto action = qobject_cast<QAction*>(sender());
    if (action) {
        auto name = action->property(CONNECTION_PROP).toString();
        if (!name.isEmpty()) {
            auto dataStore = new DataStore(App::connectionSettings(name));
            dataStore->loadAccounts(std::bind_front(&FileMenu::handleOpenResult, this));
        }
    }
}

void FileMenu::handleOpenResult(DataStore *dataStore, const QString &error) {
    if (error.isEmpty()) {
        App::addRecentName(dataStore->connectionSettings().configName());
        auto context = new UiContext(dataStore);
        context->start(appWindow()->frameGeometry());
    } else {
        QMessageBox::critical(appWindow(), tr("Connection Error"), error);
    }
}
