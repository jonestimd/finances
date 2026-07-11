#ifndef CONNECTIONDIALOG_H
#define CONNECTIONDIALOG_H

#include "service/database/connectionpool.h"
#include "ui/model/datastore.h"
#include <QComboBox>
#include <QDialog>
#include <QLabel>
#include <QMenu>

class ConnectionDialog : public QDialog {
    Q_OBJECT
public:
    enum Mode {
        Open = 1,
        Create = 2,
        OpenOrCreate = 3,
    };

private:
    Mode const mode;
    QComboBox typeInput;
    QPushButton *testButton{};
    QPushButton *openButton{};
    QPushButton *createButton{};
    QLineEdit *userInput;
    QLineEdit *passwordInput{};
    QLabel status{};
    ConnectionSettings settings;

public:
    ConnectionDialog(QWidget *parent = nullptr, Mode mode = Mode::Open);

    const ConnectionSettings connectionSettings() const;

private slots:
    void testConnection();
    void typeChanged(const QString& value);
    void inputChanged();
    void createDatabase();
    void createFailed(const QString message);
    void openDatabase();

private:
    void handleOpenResult(DataStore* dataStore, const QString& error);

    template<typename Value>
    QLineEdit* connectInput(QLineEdit* input, Value ConnectionSettings::*field, bool sqliteInput = false);
};

#endif // CONNECTIONDIALOG_H
