#ifndef FINANCES_H
#define FINANCES_H

#include <QApplication>
#include <QColor>
#include <QFont>
#include <QLabel>
#include <QLineEdit>
#include <QSettings>
#include <QToolBar>
#include <Qt>

namespace finances {
    enum ItemDataRole {
        TextHighlightRole = Qt::UserRole,
        SortRole,
        UnsavedRole,
        OptionsRole,
        ValidationMessageRole,
        ValidatorFactoryRole,
    };

    enum UnsavedState {
        AddUpdate,
        Delete,
    };

    enum FontIcon {
        AccountBalance = 0xe84f, // company
        AddCircle = 0xe147,
        AreaChart  = 0xe770, // security?
        Category = 0xe574,
        Checked = 0xe834,
        Filter = 0xe152,
        HideSource = 0xf023,
        Merge = 0xeb98,
        MergeType = 0xe252, // merge category
        // MoneyBag = 0xf3ee, // security?
        MoveItem = 0xf1ff,
        MoveDown = 0xeb61,
        MoveUp = 0xeb64,
        Person = 0xe7fd,
        Refresh = 0xe5d5,
        Save = 0xe161,
        Trash = 0xe872,
        Unchecked = 0xe835,
        Undo = 0xe166,
        Workspaces = 0xe1a0, // groups
    };

    class FontResource {
        int fontId;
        QString family;
        const char *style;
    public:
        FontResource(const char *fileName, const char *style);
        ~FontResource();

        QFont font();

        QFont font(int pointSize);
    };

    Q_GLOBAL_STATIC(FontResource, iconFont, ":/fonts/MaterialSymbolsRounded_Filled-Regular.ttf", "Regular");

    QIcon qIcon(FontIcon icon);
    QLabel* iconWidget(FontIcon icon, QWidget *parent = nullptr);
    QAction *initAction(QAction *action, FontIcon icon, const QString &text, const QString &tooltip);
    QAction *initAction(QAction *action, FontIcon icon, const QString &text, const QKeySequence &shortcut);
    QAction* iconAction(FontIcon icon, const QString &text, QObject *parent = nullptr);
    QAction* iconAction(FontIcon icon, const QString &text, const QString &shortcut, QObject *receiver, const char *slot, bool enabled = true);
    QAction* iconAction(FontIcon icon, const QString &text, QKeySequence::StandardKey shortcut, QObject *receiver = nullptr, const char *slot = nullptr, bool enabled = true);
    QAction* iconAction(const char *iconFile, const QString &text, QObject *parent = nullptr);
    QAction* iconToggle(FontIcon icon, const QString &text, const QString &shortcut, QObject *receiver, const char *slot);

    class App : public QApplication {
        Q_OBJECT
        QString userStyleSheet;
    public:
        QSettings *const settings;

        App(int &argc, char **argv);
        ~App();

    public slots:
        void updateStyleSheet(Qt::ColorScheme scheme);
    };
}

#endif // FINANCES_H
