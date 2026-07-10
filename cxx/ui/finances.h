#ifndef FINANCES_H
#define FINANCES_H

#include <QApplication>
#include <QColor>
#include <QFont>
#include <QHeaderView>
#include <QLabel>
#include <QLineEdit>
#include <QSettings>
#include <QToolBar>
#include <Qt>

class UiContext;

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
        ArrowRight = 0xf81c,
        Category = 0xe574,
        Checked = 0xe834,
        Filter = 0xe152,
        Help = 0xe887,
        HideSource = 0xf023,
        LibraryBooks = 0xe02f,
        Merge = 0xeb98,
        MergeType = 0xe252, // merge category
        MoveItem = 0xf1ff,
        MoveDown = 0xeb61,
        MoveUp = 0xeb64,
        NewWindow = 0xf710,
        OpenInNew = 0xe89e,
        Person = 0xe7fd,
        Refresh = 0xe5d5,
        Save = 0xe161,
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

    Q_GLOBAL_STATIC(FontResource, iconFont, ":/fonts/MaterialSymbolsRounded_Filled-Regular.ttf", "Regular");

    QIcon materialIcon(FontIcon icon, QColor color = {});
    QLabel* iconWidget(FontIcon icon, QWidget *parent = nullptr);
    QAction *initAction(QAction *action, FontIcon icon, const QString &text, const QString &tooltip);
    QAction *initAction(QAction *action, FontIcon icon, const QString &text, const QKeySequence &shortcut);
    QAction* iconAction(FontIcon icon, const QString &text, QObject *parent = nullptr);
    QAction* iconAction(FontIcon icon, const QString &text, const QString &shortcut, QObject *receiver, const char *slot, bool enabled = true);
    QAction* iconAction(FontIcon icon, const QString &text, QKeySequence::StandardKey shortcut, QObject *receiver = nullptr, const char *slot = nullptr, bool enabled = true);
    QAction* iconAction(const char *iconFile, const QString &text, QObject *parent = nullptr);
    QAction* iconToggle(FontIcon icon, const QString &text, const QString &shortcut, QObject *receiver, const char *slot);

    QAction *saveAction(QWidget *window, const char *invokable = "saveData");
    QAction *reloadAction(QWidget *window, const char *invokable = "loadData");

    QLineEdit* fileInput(QWidget* parent, const QString caption = {}, const QString filter = {}, const QString dir = {});
    QLineEdit* maskInput(QWidget* parent, const QString& mask);
    QLineEdit *whatsThisInput(QWidget *parent, const QString& helpText);
    QLineEdit* passwordInput(QWidget* parent);
    QFrame *separator(QFrame::Shape shape = QFrame::VLine);

    void setColumnResize(QHeaderView *viewHeader);

    class App : public QApplication {
        Q_OBJECT
        QString userStyleSheet;

    public:
        App(int &argc, char **argv);
        ~App();

        int start();

    public slots:
        void updateStyleSheet(Qt::ColorScheme scheme);
    };
}

#endif // FINANCES_H
