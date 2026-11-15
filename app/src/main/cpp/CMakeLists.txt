cmake_minimum_required(VERSION 3.22.1)
project(identra_native CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(identra_native SHARED identra_native.cpp)
target_compile_options(identra_native PRIVATE -fvisibility=hidden -fno-exceptions -Os -Wall)
target_link_options(identra_native PRIVATE -Wl,--gc-sections -Wl,-z,max-page-size=16384)
target_link_libraries(identra_native log)
