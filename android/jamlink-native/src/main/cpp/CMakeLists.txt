cmake_minimum_required(VERSION 3.22.1)
project("jamlink-native")

add_library(jamlink-native SHARED
    jamlink-jni.cpp
    jamlink-engine.cpp
)

find_library(log-lib log)

target_include_directories(jamlink-native PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(jamlink-native
    ${log-lib}
    # Oboe will be added in Phase 4
)
