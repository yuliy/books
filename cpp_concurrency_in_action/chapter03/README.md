# Chapter 3. Sharing data between threads
`std::mutex` usage basics example:
```cpp
#include <list>
#include <mutex>
#include <algorithm>

std::list<int> some_list;
std::mutex some_mutex;

void add_to_list(int new_value)
{
    std::lock_guard<std::mutex> guard(some_mutex);
    some_list.push_back(some_list);
}

bool list_contains(int value_to_find)
{
    std::lock_guard<std::mutex> guard(some_mutex);
    return std::find(
        some_list.begin(),
        some_list.end(),
        value_to_find
    ) != some_list.end();
}
```

## Deadlock
Common deadlock case is locking 2 mutexes in different orders. One way to avoid it is to always lock it in the same order. But it's not always possible. The other way is to use `std::lock()`:
```cpp
class some_big_object;
void swap(some_big_object &lhs, some_big_object &rhs);

class X
{
private:
    some_big_object some_detail;
    std::mutex m;
public:
    X(const some_big_object& sd) : some_detail(sd) {}

    friend void swap (X &lhs, X &rhs)
    {
        if (&lhs == &rhs)
            return;

        // Here both mutexes are locked using algorithm that
        // avoids deadlock.
        std::lock(lhs.m, rhs.m);

        // And here were create lock_guard instances that will
        // unlock mutex before destruction.
        // std::adopt_lock tells the guard not to acquire lock
        // (because it's already locked by `std::lock`).
        std::lock_guard<std::mutex> lock_a(lhs.m, std::adopt_lock);
        std::lock_guard<std::mutex> lock_b(rhs.m, std::adopt_lock);

        swap(lhs.some_detail, rhs.some_detail);
    }
};
```

`std::lock()` may throw exceptions.


## Guidelines for avoiding deadlock
Deadlock doesn't just occur with locks, although that's the most frequent cause; you can create deadlock with two threads and no locks just by having each thread call `join()` on the `std::thread` object for the other.

The guidelines for avoiding deadlock all boil down to one idea: don't wait for another thread if there's a chance it's waiting for you.

Guidelines for avoiding deadlock:
  * Avoid nested locks.
    <br>If you need to acquire multiple locks use `std::lock`.
  * Avoid calling user-supplied code while holding a lock.
    <br>Because internally it may acquire some lock.
  * Acquire locks in a fixed order;
  * Use a lock hierarchy.
    <br>The idea is that you divide your application into layers and identify all the mutexes that may be locked in any given layer. When code tries to lock a mutex, it isn't permitted to lock that mutex if it already holds a lock from a lower layer. You can check this at runtime by assigning layer numbers to each mutex and keeping a record of which mutexes are locked by each thread.

Example of using a lock hierarchy to prevent deadlock:
```cpp
hierarhical_mutex high_level_mutex(10000);
hierarhical_mutex low_level_mutex(5000);

int do_low_level_stuff();

int low_level_func()
{
    std::lock_guard<hierarhical_mutex> lk(low_level_mutex);
    return do_low_level_stuff();
}

void high_level_stuff(int some_param);

void high_level_func()
{
    std::lock_guard<hierarhical_mutex> lk(high_level_mutex);
    high_level_stuff(low_level_func);
}

void thread_a()
{
    high_level_func();
}

hierarhical_mutex other_mutex(100);
void do_other_stuff();

void other_stuff()
{
    high_level_func();
    do_other_stuff();
}

void thread_b()
{
    std::lock_guard<hierarhical_mutex> lk(other_mutex);
    other_stuff();
}
```

Possible implementation of `hierarhical_mutex`:
```cpp
class hierarhical_mutex
{
private:
    std::mutex internal_mutex;
    unsigned long const hierarchy_value;
    unsigned long previous_hierarchy_value;
    static thread_local unsigned long this_thread_hierarchy_value;

    void check_for_hierarchy_violation()
    {
        if (this_thread_hierarchy_value <= hierarchy_value)
            throw std::logic_error("mutex hierarchy violated");
    }

    void update_hierarchy_value()
    {
        previous_hierarchy_value = this_thread_hierarchy_value;
        this_thread_hierarchy_value = hierarchy_value;
    }

public:
    explicit hierarhical_mutex(unsigned long value)
        : hierarchy_value(value)
        , previous_hierarchy_value(0)
    {}

    void lock()
    {
        check_for_hierarchy_violation();
        internal_mutex.lock();
        update_hierarchy_value();
    }

    void unlock()
    {
        this_thread_hierarchy_value = previous_hierarchy_value;
        internal_mutex.unlock();
    }

    bool try_lock()
    {
        check_for_hierarchy_violation();
        if (!internal_mutex.try_lock())
            return false;

        update_hierarchy_value();
        return true;
    }
};

thread_local unsigned long hierarchical_mutex::this_thread_hierarchy_value(ULONG_MAX);
```

## std::unique_lock
Like `std::lock_guard`, this is a class template parameterized on the mutex type, and it also provides the same RAII-style lock management as `std::lock_guard` but with a bit more flexibility.

What's the difference with `std::lock_guard`? `std::unique_lock` gives a bit more flexibility:
  * It doesn't always own the mutex.
  * You can pass `std::defer_lock` as the second argument to indicate the the mutex should remain unlocked on construction.
  * The lock can then be acquired later by calling `std::unique_lock::lock()` or `std::lock()`.
  * It takes more space and is a fraction slower than `std::lock_guard`. The flexibility of allowing an `std::unique_lock` instance not to own the mutex comes at a price: this information has be stored and updated.

Code example:
```cpp
...
    friend void swap(X &lhs, X &rhs)
    {
        if (&lhs == &rhs)
            return;

        std::unique_lock<std::mutex> lock_a(lhs.m, std::defer_lock);
        std::unique_lock<std::mutex> lock_b(rhs.m, std::defer_lock);
        std::lock(lock_a, lock_b);

        swap(lhs.some_detail, rhs.some_detail);
    }
...
```


## Transferring mutex ownership between scopes
E.g.:
```cpp
std::unique_lock<std::mutex> get_lock()
{
    extern std::mutex some_mutex;
    std::unique_lock<std::mutex> lk(some_mutex);
    prepare_data();
    return lk;
}

void process_data()
{
    std::unique_lock<std::mutex> lk(get_lock());
    do_something();
}
```


## Locking at an appropriate granularity
> (!) In general, a lock should be held for only the minimum possible time needed to perform the required operations.

E.g.:
```cpp
void get_and_process_data()
{
    std::unique_lock<std::mutex> my_lock(the_mutex);
    some_class data_to_process=get_next_data_chunk();
    my_lock.unlock();
    result_type result=process(data_to_process);
    my_lock.lock();
    write_result(data_to_process,result);
}
```

We can avoid locking two object at the same moment by copying its values:
```cpp
class Y
{
private:
    int some_detail;
    mutable std::mutex m;
    int get_detail() const
    {
        std::lock_guard<std::mutex> lock_a(m);
        return some_detail;
    }
public:
    Y(int sd):some_detail(sd){}

    friend bool operator==(const Y &lhs, const Y &rhs)
    {
        if (&lhs == &rhs)
            return true;

        const int lhs_value = lhs.get_detail();
        const int rhs_value = rhs.get_detail();
        return lhs_value == rhs_value;
    }
};
```

But this solution may not be applicable in some cases. Because it changes the semantics of comparison. When lock both object simultaneously we check whether they're equal simultaneously. But in the later code example we may get a data race.


## Protecting shared data during initialization
Naive solution is thread-safe lazy initialization using a mutex:
```cpp
std::shared_ptr<some_resource> resource_ptr;
std::mutex resource_mutex;

void foo()
{
    std::unique_lock<std::mutex> lk(resource_mutex);
    if(!resource_ptr)
    {
        resource_ptr.reset(new some_resource);
    }
    lk.unlock();
    resource_ptr->do_something();
}
```

Trying *Double-Checked Locking* pattern:
```cpp
void undefined_behaviour_with_double_checked_locking()
{
    if (!resource_ptr)                              // (1)
    {
        std::lock_guard<std::mutex> lk(resource_mutex);
        if (!resource_ptr)                          // (2)
        {
            resource_ptr.reset(new some_resource);  // (3)
        }
    }
    resource_ptr->do_something();                   // (4)
}
```

Unfortunately code above has the potential for nasty race conditions, because of read outside the lock `(1)` isn't synchonized with the write done by another thread inside the lock `(3)`. This therefore creates a race condition that covers not just the pointer itself but also the object pointed to; even if a thread sees the pointer written by another thread, it might not see the newly created instance of `some_resource`, resulting in the call to `do_something()` `(4)`.

This is an example of the type of race condition defined as a data race by the C++ Standard and thus specified as *undefined behavior*.

The C++ Standards Committee also saw that this was an important scenario, and so the C++ Standard Library provides `std::once_flag` and `std::call_once` to handle this situation.

Solution recoded using `std::call_once`:
```cpp
std::shared_ptr<some_resource> resource_ptr;
std::once_flag resource_flag;

void init_resource()
{
    resource_ptr.reset(new some_resource);
}

void foo()
{
    std::call_once(resource_flag, init_resource);
    resource_ptr->do_something();
}
```

This pattern can also be used for lazy initialization as in the following listing:
```cpp
class X
{
private:
    connection_info connection_details;
    connection_handle connection;
    std::once_flag connection_init_flag;

    void open_connection()
    {
        connection = connection_manager.open(connection_details);
    }
public:
    X(const connection_info &connection_details_)
        : connection_details(connection_details_)
    {}

    void send_data(const data_packet &data)
    {
        std::call_once(connection_init_flag, &X::open_connection, this);
        connection.send_data(data);
    }

    data_packet receive_data()
    {
        std::call_once(connection_init_flag, &X::open_connection, this);
        return connection.receive_data();
    }
};
```

Like `std::mutex`, `std::once_flag` is neither copyable, nor movable.

In C++11 initialization of static local variables is defined to happen on exactly on thread, and no other threads will proceed until that initialization is complete. Hence, it can be used in cases where a single global instance is required:
```cpp
class my_class;

my_class& get_my_class_instance()
{
    static my_class instance;
    return instance;
}
```


## Protecting rarely updated data structures
We need some special kind of mutex that can be efficienly used in MRSW (multiple-reader single writer) scenario. The new C++ Standard Library (C++11) doesn't provide such a mutex out of the box, although one was proposed to the Standards Commitee.

> На самом деле так было на момент написания книги (стандарт 2011ого года). Но, начиная с C++17 в стандартной библиотеке появился [std::shared_mutex](https://en.cppreference.com/w/cpp/thread/shared_mutex).
