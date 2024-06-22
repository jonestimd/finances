target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/datastore.h ${CMAKE_CURRENT_LIST_DIR}/datastore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/accounttablemodel.h ${CMAKE_CURRENT_LIST_DIR}/accounttablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/formats.h ${CMAKE_CURRENT_LIST_DIR}/formats.cpp
    ${CMAKE_CURRENT_LIST_DIR}/columnadapter.h
    ${CMAKE_CURRENT_LIST_DIR}/relationcolumnadapter.h
    ${CMAKE_CURRENT_LIST_DIR}/podtablemodel.h
)
