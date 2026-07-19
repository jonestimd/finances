# cmds:
# $ cmake -S . -B build
# $ cmake --build build -v
# $ cmake --build build --target update_translations
# env:
# CMAKE_PREFIX_PATH=/opt/Qt/6.11.0/gcc_64/lib/cmake
cmake_minimum_required(VERSION 3.27)

project(finances VERSION 0.1 LANGUAGES CXX)

if (NOT DEFINED CMAKE_PREFIX_PATH)
    message(WARNING "CMAKE_PREFIX_PATH not defined, using env value")
    set(CMAKE_PREFIX_PATH "$ENV{CMAKE_PREFIX_PATH}")
endif()

if (NOT DEFINED CMAKE_C_COMPILER)
    message(WARNING "CMAKE_C_COMPILER not defined, using gcc")
    set(CMAKE_C_COMPILER gcc)
endif()

set(CMAKE_AUTORCC ON)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(CTest)
include(ExternalProject)

find_file(MAKE make ${PATH})
find_file(COPY cp ${PATH})

ExternalProject_Add(sqlite3
    GIT_REPOSITORY https://github.com/sqlite/sqlite.git
    GIT_TAG release
    CONFIGURE_COMMAND <SOURCE_DIR>/configure --disable-tcl
    BUILD_IN_SOURCE true
    BUILD_COMMAND
        ${MAKE} sqlite3.c && ${CMAKE_C_COMPILER} -g -I. -fPIC -shared ext/misc/decimal.c -o decimal.so
    BUILD_BYPRODUCTS
        <SOURCE_DIR>/decimal.so
    INSTALL_COMMAND ""
)
ExternalProject_Get_Property(sqlite3 SOURCE_DIR)
set(SQLITE_EXTENSION_DIR ${SOURCE_DIR})
add_custom_target(decimal.so
    DEPENDS sqlite3
    COMMAND ${COPY} ${SQLITE_EXTENSION_DIR}/decimal.so .
)

ExternalProject_Add(qdecimal
    GIT_REPOSITORY https://github.com/semihc/qdecimal.git
    CMAKE_ARGS -DCMAKE_PREFIX_PATH=${CMAKE_PREFIX_PATH} -DCMAKE_C_FLAGS=-fPIC
    INSTALL_COMMAND ""
    BUILD_BYPRODUCTS
        <BINARY_DIR>/decnumber/libdecnumber.a
        <BINARY_DIR>/src/libqdecimal.a
)
ExternalProject_Get_Property(qdecimal SOURCE_DIR BINARY_DIR)
set(QDECIMAL_SOURCE_DIR ${SOURCE_DIR})
set(QDECIMAL_BINARY_DIR ${BINARY_DIR})
add_library(libdecnumber STATIC IMPORTED)
add_dependencies(libdecnumber qdecimal)
set_target_properties(libdecnumber
    PROPERTIES IMPORTED_LOCATION ${QDECIMAL_BINARY_DIR}/decnumber/libdecnumber.a
)
add_library(libqdecimal STATIC IMPORTED)
add_dependencies(libqdecimal qdecimal)
set_target_properties(libqdecimal
    PROPERTIES IMPORTED_LOCATION ${QDECIMAL_BINARY_DIR}/src/libqdecimal.a
)

find_package(QT NAMES Qt6 REQUIRED COMPONENTS Widgetsl)
find_package(Qt${QT_VERSION_MAJOR} REQUIRED
    COMPONENTS Widgets Sql Concurrent Test
    OPTIONAL_COMPONENTS LinguistTools
)
find_package(SQLite3 REQUIRED)

qt_standard_project_setup(I18N_TRANSLATED_LANGUAGES en)

add_library(qtcommon OBJECT
    ${CMAKE_CURRENT_LIST_DIR}/cxx/qtcommon.cpp ${CMAKE_CURRENT_LIST_DIR}/cxx/qtcommon.h
)
target_include_directories(qtcommon PRIVATE
    $<TARGET_PROPERTY:finances,INCLUDE_DIRECTORIES>
)
add_dependencies(qtcommon libdecnumber)

set(PROJECT_RESOURCES
    ${CMAKE_CURRENT_LIST_DIR}/cxx/ui/resources/finances.qrc
    ${CMAKE_CURRENT_LIST_DIR}/cxx/ui/resources/finances.svg
    ${CMAKE_CURRENT_LIST_DIR}/cxx/ui/resources/finances.qss
    ${CMAKE_CURRENT_LIST_DIR}/cxx/ui/resources/minimal-dark.qss
    ${CMAKE_CURRENT_LIST_DIR}/cxx/ui/resources/minimal-light.qss
)

if(${QT_VERSION_MAJOR} GREATER_EQUAL 6)
    qt_add_executable(finances
        MANUAL_FINALIZATION
        ${PROJECT_RESOURCES}
    )
    add_dependencies(finances libdecnumber qdecimal decimal.so)
# Define target properties for Android with Qt 6 as:
#    set_property(TARGET finances APPEND PROPERTY QT_ANDROID_PACKAGE_SOURCE_DIR
#                 ${CMAKE_CURRENT_SOURCE_DIR}/android)
# For more information, see https://doc.qt.io/qt-6/qt-add-executable.html#target-creation
else()
    if(ANDROID)
        add_library(finances SHARED
            # ${PROJECT_SOURCES}
        )
# Define properties for Android with Qt 5 after find_package() calls as:
#    set(ANDROID_PACKAGE_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/android")
    else()
        add_executable(finances
            # ${PROJECT_SOURCES}
        )
    endif()
endif()

target_include_directories(finances PUBLIC
    ${QDECIMAL_SOURCE_DIR}/src
    ${QDECIMAL_SOURCE_DIR}/decnumber
    ${SQLite3_INCLUDE_DIR}
    ${CMAKE_HOME_DIRECTORY}/cxx
)

target_link_libraries(finances PRIVATE
    Qt${QT_VERSION_MAJOR}::Widgets
    Qt${QT_VERSION_MAJOR}::Sql
    Qt${QT_VERSION_MAJOR}::Concurrent
    ${SQLite3_LIBRARY}
    $<TARGET_OBJECTS:qtcommon>
    # comment out next 2 lines for initial build with Ninja
    libqdecimal
    libdecnumber
)

add_subdirectory(cxx/)

# Qt for iOS sets MACOSX_BUNDLE_GUI_IDENTIFIER automatically since Qt 6.1.
# If you are developing for iOS or macOS you should consider setting an
# explicit, fixed bundle identifier manually though.
if(${QT_VERSION} VERSION_LESS 6.1.0)
  set(BUNDLE_ID_OPTION MACOSX_BUNDLE_GUI_IDENTIFIER com.example.finances)
endif()
set_target_properties(finances PROPERTIES
    ${BUNDLE_ID_OPTION}
    MACOSX_BUNDLE_BUNDLE_VERSION ${PROJECT_VERSION}
    MACOSX_BUNDLE_SHORT_VERSION_STRING ${PROJECT_VERSION_MAJOR}.${PROJECT_VERSION_MINOR}
    MACOSX_BUNDLE TRUE
    WIN32_EXECUTABLE TRUE
)

include(GNUInstallDirs)
install(TARGETS finances
    BUNDLE DESTINATION .
    LIBRARY DESTINATION ${CMAKE_INSTALL_LIBDIR}
    RUNTIME DESTINATION ${CMAKE_INSTALL_BINDIR}
)

if(QT_VERSION_MAJOR EQUAL 6)
    qt_finalize_executable(finances)
endif()
