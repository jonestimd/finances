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
        Category = 0xe574,
        Checked = 0xe834,
        Filter = 0xe152,
        Person = 0xe7fd,
        Refresh = 0xe5d5,
        Save = 0xe161,
        Trash = 0xe872,
        Unchecked = 0xe835,
        Undo = 0xe166,
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
    QAction* iconAction(FontIcon icon, QString text, QObject *parent = nullptr);
    QAction* iconAction(FontIcon icon, QString text, QString shortcut, QObject *receiver, const char *slot, bool enabled = true);
    QAction* iconAction(FontIcon icon, QString text, QKeySequence::StandardKey shortcut, QObject *receiver = nullptr, const char *slot = nullptr, bool enabled = true);
    QAction* iconAction(const char *iconFile, QString text, QObject *parent = nullptr);

    class App : public QApplication {
        Q_OBJECT
        QString userStyleSheet;
    public:
        QSettings *const settings;

        App(int &argc, char **argv);

    public slots:
        void updateStyleSheet(Qt::ColorScheme scheme);
    };
}

#endif // FINANCES_H
