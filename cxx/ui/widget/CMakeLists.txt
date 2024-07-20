target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/accountswindow.h ${CMAKE_CURRENT_LIST_DIR}/accountswindow.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companieswindow.h ${CMAKE_CURRENT_LIST_DIR}/companieswindow.cpp
    ${CMAKE_CURRENT_LIST_DIR}/dialog.h ${CMAKE_CURRENT_LIST_DIR}/dialog.cpp
    ${CMAKE_CURRENT_LIST_DIR}/tableitemdelegate.h ${CMAKE_CURRENT_LIST_DIR}/tableitemdelegate.cpp
    ${CMAKE_CURRENT_LIST_DIR}/filterinput.h ${CMAKE_CURRENT_LIST_DIR}/filterinput.cpp
    ${CMAKE_CURRENT_LIST_DIR}/statusbar.h ${CMAKE_CURRENT_LIST_DIR}/statusbar.cpp
    ${CMAKE_CURRENT_LIST_DIR}/relationeditor.h ${CMAKE_CURRENT_LIST_DIR}/relationeditor.cpp
    ${CMAKE_CURRENT_LIST_DIR}/settings.h ${CMAKE_CURRENT_LIST_DIR}/settings.cpp
    ${CMAKE_CURRENT_LIST_DIR}/tablesort.h ${CMAKE_CURRENT_LIST_DIR}/tablesort.cpp
)
