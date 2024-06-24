#include "iostream"
#include <QApplication>
#include <QDecNumber.hh>
#include <QStyle>
#include <QStyleHints>

#define DB_NAME "finances_test"
#define DB_USER DB_NAME

/* sample xubuntu colors
 *** dark:
   accent: ff308cc6
   alternate-base: ff2b2b2b
   base: ff242424
   bright-text: ff4b4b4b
   button: ff323232
   button-text: fff0f0f0
   highlight: ff308cc6
   highlighted-text: fff0f0f0
   plactholder-text: 80f0f0f0
   text: fff0f0f0
   window: ff323232
   window-text: fff0f0f0
 *** light:
   accent: ff308cc6
   alternate-base: fff7f7f7
   base: ffffffff
   bright-text: ffffffff
   button: ffefefef
   button-text: ff000000
   highlight: ff308cc6
   highlighted-text: ffffffff
   plactholder-text: 80000000
   text: ff000000
   window: ffefefef
   window-text: ff000000
 */
int main(int argc, char *argv[])
{
    QApplication app(argc, argv);

    QStyle *style = app.style();
    auto palette = style->standardPalette();
    std::cout << "accent: " << std::hex << palette.accent().color().rgba() << '\n';
    std::cout << "alternate-base: " << std::hex << palette.alternateBase().color().rgba() << '\n';
    std::cout << "base: " << std::hex << palette.base().color().rgba() << '\n';
    std::cout << "bright-text: " << std::hex << palette.brightText().color().rgba() << '\n';
    std::cout << "button: " << std::hex << palette.button().color().rgba() << '\n';
    std::cout << "button-text: " << std::hex << palette.buttonText().color().rgba() << '\n';
    std::cout << "highlight: " << std::hex << palette.highlight().color().rgba() << '\n';
    std::cout << "highlighted-text: " << std::hex << palette.highlightedText().color().rgba() << '\n';
    std::cout << "plactholder-text: " << std::hex << palette.placeholderText().color().rgba() << '\n';
    std::cout << "text: " << std::hex << palette.text().color().rgba() << '\n';
    std::cout << "window: " << std::hex << palette.window().color().rgba() << '\n';
    std::cout << "window-text: " << std::hex << palette.windowText().color().rgba() << '\n';
    return 0;
}
