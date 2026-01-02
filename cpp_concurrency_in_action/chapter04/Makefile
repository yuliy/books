
define build_and_run
	g++ -Wall -std=c++17 -o x $1 && ./x
endef

all: spawn_lots_of_threads

spawn_lots_of_threads:
	$(call build_and_run, spawn_lots_of_threads.cpp)

