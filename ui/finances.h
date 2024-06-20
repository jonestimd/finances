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

namespace Finances {
    enum ItemDataRole {
        TextHighlight = Qt::UserRole,
        SortRole,
    };

    enum FontIcon {
        Filter = 0xe152,
    };

    QLabel* iconWidget(FontIcon icon, QWidget *parent = nullptr);

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
