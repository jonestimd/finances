qt_add_executable(test_decimal test_decimal.cpp)
set_target_properties(test_decimal PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY $<TARGET_PROPERTY:finances,BINARY_DIR>
)

add_dependencies(test_decimal qdecimal)

set_target_properties(test_decimal PROPERTIES
    AUTOMOC ON
    WIN32_EXECUTABLE TRUE
    MACOSX_BUNDLE TRUE
)

target_include_directories(test_decimal PRIVATE
    ${QDECIMAL_SOURCE_DIR}/src
    ${QDECIMAL_SOURCE_DIR}/decnumber
)

target_link_libraries(test_decimal PRIVATE
    Qt6::Core
    Qt6::Gui
    Qt6::Test
    Qt6::Widgets
    ${QDECIMAL_BINARY_DIR}/src/libqdecimal.a
    ${QDECIMAL_BINARY_DIR}/decnumber/libdecnumber.a
)

add_test(NAME test_decimal COMMAND test_decimal)

qt_add_library(finances-uitest OBJECT
    ${CMAKE_CURRENT_LIST_DIR}/uitest.h ${CMAKE_CURRENT_LIST_DIR}/uitest.cpp
)

target_link_libraries(finances-uitest PUBLIC
    Qt${QT_VERSION_MAJOR}::Core
)

target_include_directories(finances-uitest PRIVATE
    $<TARGET_PROPERTY:finances,INCLUDE_DIRECTORIES>
)
