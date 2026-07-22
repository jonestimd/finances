target_sources(finances-ui
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/accounttablemodel.h ${CMAKE_CURRENT_LIST_DIR}/accounttablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/accountstore.h ${CMAKE_CURRENT_LIST_DIR}/accountstore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/adapteritemmodel.h ${CMAKE_CURRENT_LIST_DIR}/adapteritemmodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/categorystore.h ${CMAKE_CURRENT_LIST_DIR}/categorystore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/categorytablemodel.h ${CMAKE_CURRENT_LIST_DIR}/categorytablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/columnadapter.h ${CMAKE_CURRENT_LIST_DIR}/columnadapter.cpp
    ${CMAKE_CURRENT_LIST_DIR}/comboboxmodel.h ${CMAKE_CURRENT_LIST_DIR}/comboboxmodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companystore.h ${CMAKE_CURRENT_LIST_DIR}/companystore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companytablemodel.h ${CMAKE_CURRENT_LIST_DIR}/companytablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/datastore.h ${CMAKE_CURRENT_LIST_DIR}/datastore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/detailcolumnadapter.h ${CMAKE_CURRENT_LIST_DIR}/detailcolumnadapter.cpp
    ${CMAKE_CURRENT_LIST_DIR}/entitystore.h ${CMAKE_CURRENT_LIST_DIR}/entitystore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/formats.h ${CMAKE_CURRENT_LIST_DIR}/formats.cpp
    ${CMAKE_CURRENT_LIST_DIR}/groupstore.h ${CMAKE_CURRENT_LIST_DIR}/groupstore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/grouptablemodel.h ${CMAKE_CURRENT_LIST_DIR}/grouptablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/payeestore.h ${CMAKE_CURRENT_LIST_DIR}/payeestore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/payeetablemodel.h ${CMAKE_CURRENT_LIST_DIR}/payeetablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/poditemmodel.h
    ${CMAKE_CURRENT_LIST_DIR}/podtablemodel.h
    ${CMAKE_CURRENT_LIST_DIR}/relationcolumnadapter.h
    ${CMAKE_CURRENT_LIST_DIR}/securitystore.h ${CMAKE_CURRENT_LIST_DIR}/securitystore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/securitytablemodel.h ${CMAKE_CURRENT_LIST_DIR}/securitytablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/sortfilterproxymodel.h ${CMAKE_CURRENT_LIST_DIR}/sortfilterproxymodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/statusmessagestore.h ${CMAKE_CURRENT_LIST_DIR}/statusmessagestore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactiondetailstore.h ${CMAKE_CURRENT_LIST_DIR}/transactiondetailstore.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactiontablemodel.h ${CMAKE_CURRENT_LIST_DIR}/transactiontablemodel.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactionstore.h ${CMAKE_CURRENT_LIST_DIR}/transactionstore.cpp
)

add_subdirectory(tests/)
