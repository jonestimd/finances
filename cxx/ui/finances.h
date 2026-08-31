#ifndef FINANCES_H
#define FINANCES_H

#include "service/database/connectionpool.h"
#include <QApplication>
#include <QColor>
#include <QFont>
#include <QHeaderView>
#include <QLabel>
#include <QLineEdit>
#include <QToolBar>
#include <Qt>

#define APP_NAME "finances"

namespace finances {
    enum ItemDataRole {
        TextHighlightRole = Qt::UserRole,
        SortRole,
        UnsavedRole,
        OptionsRole,
        ValidationMessageRole,
        ValidatorFactoryRole,
        EntityIdRole,
        EntityPtrRole,
        AltDisplayRole,
    };

    enum TextHighlight {
        Accent = 0x01,
        Dimmed = 0x02,
    };

    enum UnsavedState {
        Add = 0x01,
        Delete = 0x02,
        Update = 0x04,
    };

    enum FontIcon {
        AccountBalance = 0xe84f, // company
        AddCircle = 0xe147,
        AreaChart  = 0xe770,
        ArrowSplit = 0xe985, // stock split
        Category = 0xe574,
        Checked = 0xe834,
        ClockArrowDown = 0xf382,
        ClockArrowUp = 0xf381,
        Filter = 0xe152,
        Help = 0xe887,
        HideSource = 0xf023,
        Input = 0xe890, // goto transaction
        LibraryBooks = 0xe02f,
        Merge = 0xeb98,
        MergeType = 0xe252, // merge category
        MoveItem = 0xf1ff,
        MoveDown = 0xeb61,
        MoveUp = 0xeb64,
        NewWindow = 0xf710,
        OpenInNew = 0xe89e,
        Person = 0xe7fd,
        PlaylistRemove= 0xeb80, // discard lots
        Refresh = 0xe5d5,
        RightBlackArrow = 0x2b95,
        Save = 0xe161,
        Search = 0xe8b6,
        Sort = 0xe164, // highest/lowest price
        Stacks = 0xf500, // security lots
        Table = 0xf191,
        Trash = 0xe872,
        Unchecked = 0xe835,
        Undo = 0xe166,
        Visibility = 0xe8f4,
        Workspaces = 0xe1a0, // groups
        None = ' ',
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

    struct MaterialIcon {
        const FontIcon symbol;
        const FontIcon overlay;
        const bool mirrorY;

        MaterialIcon(FontIcon symbol, FontIcon overlay = None);
        MaterialIcon(FontIcon symbol, bool mirrorY);
    };

    Q_GLOBAL_STATIC(FontResource, iconFont, ":/fonts/MaterialSymbolsRounded[FILL,GRAD,opsz,wght].woff2", "Regular");

    QIcon materialIcon(MaterialIcon icon, QColor color = {});
    QLabel* iconWidget(FontIcon icon, QWidget *parent = nullptr);
    QAction* initAction(QAction *action, MaterialIcon icon, const QString &text, const QString &tooltip);
    QAction* initAction(QAction *action, QIcon icon, const QString &text, const QString &tooltip);
    QAction* initAction(QAction *action, MaterialIcon icon, const QString &text, const QKeySequence &shortcut);
    QAction* initAction(QAction *action, QIcon icon, const QString &text, const QKeySequence &shortcut);
    QAction* iconAction(MaterialIcon icon, const QString &text, QObject *parent = nullptr);
    QAction* iconAction(MaterialIcon icon, const QString &text, const QString &shortcut, QObject *receiver, const char *slot, bool enabled = true);
    QAction* iconAction(MaterialIcon icon, const QString &text, QKeySequence::StandardKey shortcut, QObject *receiver = nullptr, const char *slot = nullptr, bool enabled = true);
    QAction* iconAction(const char *iconFile, const QString &text, QObject *parent = nullptr);
    QAction* iconToggle(MaterialIcon icon, const QString &text, const QString &shortcut, QObject *receiver, const char *slot);

    QAction *saveAction(QWidget *window, const char *invokable = "saveData");
    QAction *reloadAction(QWidget *window, const char *invokable = "loadData");

    /** @return `true` if `name` already has one of the `extensions`. */
    bool ensureExtension(QString& name, const QList<QString> extensions);

    QLineEdit* openFileInput(QWidget* parent, const QString caption = {}, const QString filter = {});
    QLineEdit* saveFileInput(QWidget* parent, const QString caption = {}, const QString filter = {}, bool* replaceConfirmed = nullptr, bool* create = nullptr);
    QLineEdit* maskInput(QWidget* parent, const QString& mask);
    QLineEdit *whatsThisInput(QWidget *parent, const QString& helpText);
    QLineEdit* passwordInput(QWidget* parent);
    QFrame *separator(QFrame::Shape shape = QFrame::VLine);

    QFont boldFont();

    void setColumnResize(QHeaderView *viewHeader);

    class App : public QApplication {
        Q_OBJECT
        QString userStyleSheet;

    public:
        App(int &argc, char **argv);
        ~App();

        int start();

        static ConnectionSettings connectionSettings(const QString &name);
        static void addConnection(const ConnectionSettings& settings);
        static void addRecentName(const QString& name);
        static QStringList getRecentNames();

        static QVariant lastViewedAccount(QString connectionName);
        static void setLastViewedAccount(const QVariant &id, const QString &connectionName);

    signals:
        void recentAdded();

    public slots:
        void updateStyleSheet(Qt::ColorScheme scheme);
    };
}

#endif // FINANCES_H
