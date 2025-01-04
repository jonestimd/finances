#ifndef CATEGORYPARENTDIALOG_H
#define CATEGORYPARENTDIALOG_H

#include "service/model/category.h"
#include "relationeditor.h"
#include "ui/model/categorystore.h"
#include <QDialog>

class CategoryParentDialog : public QDialog
{
    Q_OBJECT
    const Category *category;
    RelationEditor *parentInput;
    QPushButton *saveButton;

public:
    CategoryParentDialog(QWidget *parent, const CategoryStore *store, const Category *category);

    QVariant parentId() const;

private Q_SLOTS:
    void inputChanged();
};

#endif // CATEGORYPARENTDIALOG_H
