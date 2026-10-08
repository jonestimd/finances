# env:
# CMAKE_PREFIX_PATH=/opt/Qt/6.11.0/gcc_64/lib/cmake
# cmds:
### use development Qt libs
# $ cmake -G Ninja -DCMAKE_PREFIX_PATH=/opt/Qt/6.11.1/gcc_64 -S . -B build
### use system Qt libs
# $ cmake -G Ninja -DCMAKE_PREFIX_PATH=/usr -S . -B build
### compile
# $ cmake --build build -v
### generate translation files
# $ cmake --build build --target update_translations
cmake_minimum_required(VERSION 3.27)

project(finances VERSION 2.0.0 LANGUAGES CXX)
add_compile_definitions(APP_VERSION="${CMAKE_PROJECT_VERSION}" SQLITE_CORE)

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
    GIT_SHALLOW true
    CONFIGURE_COMMAND <SOURCE_DIR>/configure --disable-tcl
    BUILD_IN_SOURCE true
    BUILD_COMMAND
        ${MAKE} sqlite3.c && ${CMAKE_C_COMPILER} -DSQLITE_CORE -I. -c -g -o decimal.o ext/misc/decimal.c
    BUILD_BYPRODUCTS
        <SOURCE_DIR>/decimal.o
    INSTALL_COMMAND ""
)
ExternalProject_Get_Property(sqlite3 SOURCE_DIR)
set(SQLITE_EXTENSION_DIR ${SOURCE_DIR})

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

find_package(QT NAMES Qt6 REQUIRED COMPONENTS Widgets)
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
    set_property(TARGET finances PROPERTY OUTPUT_NAME "finances-qt")
    add_dependencies(finances libdecnumber qdecimal sqlite3)
# Define target properties for Android with Qt 6 as:
#    set_property(TARGET finances APPEND PROPERTY QT_ANDROID_PACKAGE_SOURCE_DIR
#                 ${CMAKE_CURRENT_SOURCE_DIR}/android)
# For more information, see https://doc.qt.io/qt-6/qt-add-executable.html#target-creation
else()
    if(ANDROID)
        add_library(finances SHARED)
# Define properties for Android with Qt 5 after find_package() calls as:
#    set(ANDROID_PACKAGE_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/android")
    else()
        add_executable(finances)
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
    $<TARGET_OBJECTS:sqlite_finances>
    libqdecimal
    libdecnumber
    finances-ui
    finances-service
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

install(TARGETS finances sqlite_finances_ext)
install(FILES "package/linux/Finances-qt.desktop" DESTINATION "${CMAKE_INSTALL_DATADIR}/applications")
install(FILES "package/linux/finances-qt.png" DESTINATION "${CMAKE_INSTALL_DATADIR}/icons")

qt_finalize_executable(finances)

set(CPACK_GENERATOR DEB)
if(${CMAKE_PREFIX_PATH} STREQUAL "/usr")
    message(NOTICE "using system Qt libs")
    set(CPACK_PACKAGE_NAME finances-qt)
    string(JOIN "," DEBIAN_DEPENDS
        "libqt6sql6 (>= ${QT_VERSION})"
        "libqt6sql6-psql (>= ${QT_VERSION})"
        "libqt6sql6-mysql (>= ${QT_VERSION})"
        "libqt6widgets6 (>= ${QT_VERSION})"
    )
    set(CPACK_DEBIAN_PACKAGE_DEPENDS "${DEBIAN_DEPENDS}")
    set(CPACK_DEBIAN_PACKAGE_REPLACES "finances-qt-all, finances-qt (<< 2.0.0)")
    set(CPACK_DEBIAN_PACKAGE_BREAKS "finances-qt-all, finances-qt (<< 2.0.0)")
else()
    message(NOTICE "including Qt libs")
    set(CPACK_PACKAGE_NAME finances-qt-all)
    set(CPACK_DEBIAN_PACKAGE_DEPENDS "libxcb-cursor0 (>= 0.0.99), libxcb-icccm4 (>= 0.4.2), libxcb-keysyms1 (>= 0.4.1)")
    set(CPACK_DEBIAN_PACKAGE_REPLACES finances-qt)
    set(CPACK_DEBIAN_PACKAGE_BREAKS finances-qt)
    set_property(TARGET finances PROPERTY INSTALL_RPATH "/opt/finances/lib")
    # includes Qt libs in .deb file
    # qt_generate_deploy_app_script(TARGET finances OUTPUT_SCRIPT qt_install_script NO_TRANSLATIONS NO_PLUGINS)
    # above function would include unwanted qt.conf
    qt6_generate_deploy_script(TARGET finances OUTPUT_SCRIPT qt_install_script CONTENT "
qt6_deploy_runtime_dependencies(
    EXECUTABLE \"$<TARGET_FILE:finances>\"
    NO_TRANSLATIONS
)
")
    install(SCRIPT ${qt_install_script})

    list(APPEND CPACK_GENERATOR AppImage)
    set(CPACK_PROJECT_CONFIG_FILE "${CMAKE_CURRENT_LIST_DIR}/package/linux/CPackOptions.cmake")
endif()

set(CPACK_PACKAGE_VERSION ${CMAKE_PROJECT_VERSION})
set(CPACK_PACKAGE_CONTACT "jonestimd@users.noreply.github.com")
set(CPACK_DEBIAN_PACKAGE_DESCRIPTION "Application for tracking personal finances in a SQL database.")
set(CPACK_DEBIAN_PACKAGE_SECTION "Financial Management")
set(CPACK_DEBIAN_PACKAGE_HOMEPAGE "https://github.com/jonestimd/finances")
set(CPACK_DEBIAN_PACKAGE_ARCHITECTURE amd64)
include(CPack)
