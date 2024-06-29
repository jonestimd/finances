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
        TextHighlight = Qt::UserRole,
        SortRole,
        Unsaved,
        ValidatorFactory,
    };

    enum FontIcon {
        AccountBalance = 0xe84f, // company
        Filter = 0xe152,
        Save = 0xe161,
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

    Q_GLOBAL_STATIC(FontResource, iconFont, ":/fonts/MaterialIcons-Regular.ttf", "Regular");

    QLabel* iconWidget(FontIcon icon, QWidget *parent = nullptr);
    QAction* iconAction(FontIcon icon, QString text, QWidget *parent = nullptr);
    QAction* iconAction(FontIcon icon, QString text, QString shortcut, QWidget *parent = nullptr);
    QAction* iconAction(FontIcon icon, QString text, QKeySequence::StandardKey shortcut, QWidget *parent = nullptr);
    QAction* iconAction(const char *iconFile, QString text, QWidget *parent = nullptr);
    QAction* iconAction(const char *iconFile, QString text, QString shortcut, QWidget *parent = nullptr);

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
