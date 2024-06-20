#include "finances.h"
#include <QFile>
#include <QFontDatabase>
#include <QIcon>
#include <QPalette>
#include <QStyleHints>

QString readStyles(const QString &fileName) {
    QFile file(fileName);
    file.open(QFile::ReadOnly);
    return QString(file.readAll());
}

class FontResource {
    int fontId;
    QString family;
    const char *style;
public:
    FontResource(const char *fileName, const char *style) : style{style} {
        fontId = QFontDatabase::addApplicationFont(fileName);
        auto fontFamilies = QFontDatabase::applicationFontFamilies(fontId);
        family = fontFamilies.first();
    }

    ~FontResource() {
        QFontDatabase::removeApplicationFont(fontId);
    }

    QFont font(int pointSize) {
        return QFontDatabase::font(family, style, pointSize);
    }
};

Q_GLOBAL_STATIC(FontResource, iconFont, ":/fonts/MaterialIcons-Regular.ttf", "Light");

namespace Finances {
    QLabel* iconWidget(FontIcon icon, QWidget *parent) {
        auto label = new QLabel(parent);
        label->setFont(iconFont->font(label->font().pointSize() * 2));
        label->setText(QChar{icon});
        return label;
    }

    App::App(int &argc, char **argv)
        : QApplication(argc, argv),
        userStyleSheet{""},
        settings{new QSettings(QSettings::IniFormat, QSettings::UserScope, "finances", "finances", this)}
    {
        setWindowIcon(QIcon(":/images/finances.svg"));
        auto styleFile = styleSheet();
        if (!styleFile.isEmpty()) userStyleSheet = readStyles(styleFile.replace(0, 8, ""));
        updateStyleSheet(styleHints()->colorScheme());
        connect(styleHints(), SIGNAL(colorSchemeChanged(Qt::ColorScheme)), this, SLOT(updateStyleSheet(Qt::ColorScheme)));
    }

    void App::updateStyleSheet(Qt::ColorScheme scheme) {
        if (scheme == Qt::ColorScheme::Dark) {
            auto styles = readStyles(":/styles/dark.qss");
            setStyleSheet(styles + "\n" + userStyleSheet);
        }
        else if (scheme == Qt::ColorScheme::Light) {
            auto styles = readStyles(":/styles/light.qss");
            setStyleSheet(styles + "\n" + userStyleSheet);
        }
    }
}
