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
    QLineEdit *adminUserInput{};
    QLabel status{};
    AdminConnectionSettings settings;
    bool replaceConfirmed{false};

public:
    ConnectionDialog(QWidget *parent = nullptr, Mode mode = Mode::Open);

private slots:
    void testConnection();
    void typeChanged(const QString& value);
    void inputChanged(QWidget* widget);
    void setStatus(const QString message);
    void createDatabase();
    void openDatabase();

private:
    Q_SIGNAL void statusChanged(const QString message);

    void handleOpenResult(DataStore* dataStore, const QString& error);

    template<class Settings, class Value>
    QLineEdit* connectInput(QLineEdit* input, Value Settings::*field, const QString& name = "");
};

#endif // CONNECTIONDIALOG_H
