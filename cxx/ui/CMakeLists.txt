qt_add_library(finances-ui OBJECT
    ${CMAKE_CURRENT_LIST_DIR}/finances.h ${CMAKE_CURRENT_LIST_DIR}/finances.cpp
    ${CMAKE_CURRENT_LIST_DIR}/uicontext.h ${CMAKE_CURRENT_LIST_DIR}/uicontext.cpp
)

add_dependencies(finances-ui libdecnumber)

target_link_libraries(finances-ui PUBLIC
    Qt${QT_VERSION_MAJOR}::Core
    Qt${QT_VERSION_MAJOR}::Gui
    Qt${QT_VERSION_MAJOR}::Widgets
    ${QDECIMAL_BINARY_DIR}/src/libqdecimal.a
    ${QDECIMAL_BINARY_DIR}/decnumber/libdecnumber.a
)

target_include_directories(finances-ui PRIVATE
    $<TARGET_PROPERTY:finances,INCLUDE_DIRECTORIES>
)

add_subdirectory(model/)
add_subdirectory(validation/)
add_subdirectory(widget/)
