
define build_and_run
	g++ -Wall -std=c++17 -o x $1 && ./x
endef

all: check_if_argument_is_copied call_object_method_in_thread hardware_concurrency thread_id

check_if_argument_is_copied:
	$(call build_and_run, check_if_argument_is_copied.cpp)

call_object_method_in_thread:
	$(call build_and_run, call_object_method_in_thread.cpp)

hardware_concurrency:
	$(call build_and_run, hardware_concurrency.cpp)

thread_id:
	$(call build_and_run, thread_id.cpp)
