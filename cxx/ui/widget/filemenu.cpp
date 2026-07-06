#include "connectiondialog.h"
#include "filemenu.h"
#include <QComboBox>
#include <QDialogButtonBox>
#include <QFileDialog>
#include <QFormLayout>
#include <QPushButton>
#include <QToolButton>

namespace filemenu {
    using namespace finances;

    class FileAction : public QAction { // TODO add dialogAction() factory to finances.cpp
    public:
        FileAction(const QString& name, const QKeySequence& shortcut, QWidget* window)
            : QAction(window)
        {
            initAction(this, FontIcon::None, name, shortcut);
            connect(this, &FileAction::triggered, [=]() {
                ConnectionDialog dialog{window};
                dialog.exec();
            });
        };
    };
}

using namespace filemenu;

FileMenu::FileMenu(AppWindow* window)
    : QMenu(tr("&File"), window)
{
    // addAction(new FileAction(tr("&New File..."), QKeyCombination{}, window));
    addAction(new FileAction(tr("&Open File..."), QKeyCombination{Qt::ControlModifier, Qt::Key_O}, window));
}
