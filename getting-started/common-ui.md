---
title: Using the UI
sections:
- Editing data
- Data filter
- Keyboard shortcuts
---

# Using the UI
The following sections describe common features of the application windows.

## Editing data
Changes to data in the main application windows are not persisted
to the database until the *Save* action is invoked (by clicking the
![Save]({{ "/assets/images/save.png" | relative_url }}){:.button} button or
typing `ctrl+S`).  Table cells with unsaved additions or updates are
<span class="pending-change">highlighted</span> with a blue background and
table cells with unsaved deletes are <span class="pending-delete">highlighted</span>
with a red background and strike-through text.  Invoking the *Reload* or *Refresh* action
(by clicking the ![Reload]({{ "/assets/images/reload.png" | relative_url }}){:.button}
button or typing `ctrl+R`) will reload the data in the window, discarding unsaved changes.

Invalid data in a table cell or a dialog field is indicated by a
![red x](invalid_data_24px.svg "error indicator"){:.icon}.  A description of the
validation error is displayed in a tooltip when the mouse cursor hovers over the
error indicator.  For dialog fields, the error description is also displayed at
the bottom of the dialog.  Saving data is disabled while there is a validation error.

## Data filter ![](filter.png){:.filter}
Each of the main application windows includes a filter field that can be used
to filter the data displayed in the window.  When text is entered in the filter field,
only rows where one of the columns contains the specified text will be displayed.
The matching text in the rows will be <span class="filter-match">highlighted</span>
with a yellow background.  Complex filters can be created using the `&` (and)
and `|` (or) operators to combine search strings.  Parentheses can also be
used to group expressions within a complex filter.  Hold down the `ctrl` key
when typing an operator or grouping parenthesis, otherwise the character will
be interpreted as part of the search string.

## Keyboard shortcuts
Keyboard shortcuts for toolbar actions are shown in the tooltips for the
toolbar buttons and on the corresponding menu entries.  The following keys
can also be used within the tables in the application windows.

| Key | Action |
|---|---|
| `Esc` | start editing the selected cell or cancel editing the cell |
| `F2` | start editing the selected cell |
| `Backspace` | start editing the selected cell by deleting the last character |
| `Del` | start editing the selected cell by deleting the first character |
| `ctrl-Z` | revert unsaved changes to the selected cell or row |
| `ctrl-F` | transfer keyboard focus to the filter field |
{:.key-shortcuts}