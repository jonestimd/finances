target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/datastore.h ${CMAKE_CURRENT_LIST_DIR}/datastore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/accounttablemodel.h ${CMAKE_CURRENT_LIST_DIR}/accounttablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/categorytablemodel.h ${CMAKE_CURRENT_LIST_DIR}/categorytablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companytablemodel.h ${CMAKE_CURRENT_LIST_DIR}/companytablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/comboboxmodel.h ${CMAKE_CURRENT_LIST_DIR}/comboboxmodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/formats.h ${CMAKE_CURRENT_LIST_DIR}/formats.cpp
    ${CMAKE_CURRENT_LIST_DIR}/adaptertablemodel.h ${CMAKE_CURRENT_LIST_DIR}/adaptertablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/columnadapter.h
    ${CMAKE_CURRENT_LIST_DIR}/relationcolumnadapter.h
    ${CMAKE_CURRENT_LIST_DIR}/podtablemodel.h
    ${CMAKE_CURRENT_LIST_DIR}/payeetablemodel.h ${CMAKE_CURRENT_LIST_DIR}/payeetablemodel.cpp
)

add_subdirectory(tests/)
