target_sources(finances
  PRIVATE
    ${CMAKE_CURRENT_LIST_DIR}/accountdao.h ${CMAKE_CURRENT_LIST_DIR}/accountdao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/categorydao.h ${CMAKE_CURRENT_LIST_DIR}/categorydao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/connectionpool.h ${CMAKE_CURRENT_LIST_DIR}/connectionpool.cpp
    ${CMAKE_CURRENT_LIST_DIR}/companydao.h ${CMAKE_CURRENT_LIST_DIR}/companydao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/dbdialect.h ${CMAKE_CURRENT_LIST_DIR}/dbdialect.cpp
    ${CMAKE_CURRENT_LIST_DIR}/entitydao.h
    ${CMAKE_CURRENT_LIST_DIR}/transactiongroupdao.h ${CMAKE_CURRENT_LIST_DIR}/transactiongroupdao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/mapping.h ${CMAKE_CURRENT_LIST_DIR}/mapping.cpp
    ${CMAKE_CURRENT_LIST_DIR}/payeedao.h ${CMAKE_CURRENT_LIST_DIR}/payeedao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/securitydao.h ${CMAKE_CURRENT_LIST_DIR}/securitydao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/sql.h ${CMAKE_CURRENT_LIST_DIR}/sql.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactiondetaildao.h ${CMAKE_CURRENT_LIST_DIR}/transactiondetaildao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/transactiondao.h ${CMAKE_CURRENT_LIST_DIR}/transactiondao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/securitylotdao.h ${CMAKE_CURRENT_LIST_DIR}/securitylotdao.cpp
    ${CMAKE_CURRENT_LIST_DIR}/stocksplitdao.h ${CMAKE_CURRENT_LIST_DIR}/stocksplitdao.cpp
)
