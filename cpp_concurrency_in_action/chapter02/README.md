# Chapter 2. Managing threads

## Создание нового потока
Starting a thread using the `C++ Thread Library` always boils down to constructing an `std::thread` object:
```cpp
void do_some_work();
std::thread my_thread(do_some_work);
```

Any callable can be used:
```cpp
class background_task
{
public:
    void operator()() const
    {
        // ...
    }
};

background_task f;
std::thread my_thread(f);
```

> **!ACHTUNG!** In this case, the supplied function object is **copied** into the storage belonging to the newly created thread of execution and invoked from there. It's therefore essential that the copy behave equivalently to the original.

## C++ most vexing parse
Avoid "C++'s most vexing parse. E.g. this:
```cpp
std::thread my_thread(backgound_task());
```
declares a function `my_thread` that takes a single parameter (of type pointer to a function taking to parameters and returning a `background_task` object) and returns an `std::thread` object. You can avoid it these ways:
```cpp
// (1)
std::thread my_thread( (background_task()) );

// (2)
std::thread my_thread{background_task()};
```

Lambda expression may also be used:
```cpp
std::thread my_thread([](
    // ...
));
```

## std::thread should be either joined or detached before beeing destructed
Before `std::thread` is destroyed you are to decide whether to `join()` (i.e. wait for completion) or `detach()` (i.e. leave it to run on its own) a thread. Otherwise your program will be terminated.

You can call `join()` only once for a given thread. Once you've called `join()`, the `std::thread` object is no longer joinable, and `joinable()` will return `false`.

С `detach()` та же история. Его можно вызывать только если экземпляр `std::thread` является joinable. После вызова он становится не joinable.

If you're going to execute some code befure calling `join()` or `detach()` you should be aware of exception. Otherwise your program will be terminated:
```cpp
struct func;

void f()
{
    // ...
    func my_func();
    std::thread t(my_func);
    try
    {
        do_something_in_current_thread();
    }
    catch (...)
    {
        t.join();
    }
    t.join();
}
```


## Using RAII to avoid program termination
Evedently it's an error-prone approach. A better solution is to use RAII idiom:
```cpp
class thread_guard
{
    std::thread& t;
public:
    explicit thread_guard(std::thread& t_)
        : t(t_)
    {}

    ~thread_guard()
    {
        if (t.joinable())
        {
            t.join();
        }
    }

    thread_guard(const thread_guard&) = delete;
    thread_guard& operator=(const thread_guard&) = delete;
};

struct func;

void f()
{

    func my_func();
    std::thread t(my_func);
    thread_guard g(t);
    do_something_in_current_thread();
}
```

## Passing arguments to a thread function
Аргументы можно передавать. При этом важно понимать, что по дефолту они копируются. Так происходит даже если функция потока принимает в качестве аргумента ссылку. Например такой код:
```cpp
struct A
{
    A() { cout << "A()" << endl; }
    A(const A&) { cout << "A(const A&)" << endl; }
};

struct B
{
    B() { cout << "B()" << endl; }
    B(const B&) { cout << "B(const B&)" << endl; }
};


void func(A value, const B& ref)
{
    cout << "func()" << endl;
}

int main()
{
    A a;
    B b;
    std::thread t(func, a, b);
    t.join();
    return 0;
}


```

у меня выдал следующий выхлоп при запуске:
```cpp
A()
B()
A(const A&)
B(const B&)
A(const A&)
B(const B&)
A(const A&)
func()
```

В этом месте можно наступить на разные грабли. Например если функция принимает `std::string`, а при создании потока мы передаём `const char*`:
```cpp
void f(int i, const std::string &s);

void oops(int some_params)
{
    char buffer[1024];
    sptrinf(buffer, "%i", some_param);
    std::thread t(f, 3, buffer);
    t.detach();
}
```


## Передача аргумента в функцию потока по ссылке
Может быть и обратная ситуация - когда функция принимает аргумент по ссылке. И она должна его изменить. Но при запуске функции в потоке меняется копия аргумента. Это можно решить например с помощью `std::ref()`:
```cpp
void update_data_for_widget(widget_id w, widget_data& data);

std::thread t(update_data_for_widget, w, std::ref(data));
```

Вызов метода экземпляра класса в потоке аналогично `std::bind()`:
```cpp
struct A
{
    A() { cout << "A::A()" << endl; }
    A(const A&) { cout << "A::A(const A&)" << endl; }

    void do_smth(int some_arg)
    {
        cout << "A::do_smth(" << some_arg << ")" << endl;
    }
};


int main()
{
    A a;
    int arg = 123;
    std::thread t(&A::do_smth, &a, arg);
    t.join();
    return 0;
}
```


## Move аргумента в функцию потока по ссылке
```cpp
void process_big_object(std::unique_ptr<big_object>);

std::unique_ptr<big_object> p(new big_object);
p->prepare_data(42);
std::thread t(process_big_object, std::move(p));
```

Internally argument will be moved automatically if they're movable.


## std::thread is movable
BTW `std::thread` is a non-copyable and movable. Hence at most one thread is associated with `std::thread` instance.

It allows us to make another RAII helper class:
```cpp
class scoped_thread
{
private:
    std::thread t;
public:
    explicit scoped_thread(std::thread t_)
        : t(std::move(t_))
    {}
    scoped_thread(const scoped_thread&) = delete;
    scoped_thread& operator=(const scoped_thread&) = delete;

    ~scoped_thread()
    {
        t.join();
    }
};

struct func;

void f()
{
    int some_local_state;
    scoped_thread t(std::thread(func(some_local_state)));
    do_something_in_current_thread();
}
```

Странно, что в `std` я не нашел аналогов ни `thread_guard`, ни `scoped_thread`. В С++20 есть `std::jthread`. И всё.

Однако в Boost есть всё необходимо: см. [Scoped Threads](https://www.boost.org/doc/libs/1_53_0/doc/html/thread/ScopedThreads.html).


## Choosing the number of threads at runtime
В стандартной библиотеке есть функция `std::thread::hardware_concurrency()` - она возвращает кол-во потоков, которое реально можно запустить так, чтобы распараллелить выполнение.

На моём 8-ядерном MacBook Pro 13'' M1 эта функция возвращает 8.


## std::thread::id

`std::thread::id` представляет собой строгово упорядоченное множество + для него определена операция `std::hash<std::thread::id>()` (т.е. его можно хранить в хеш-таблицах). Вывод для потоков разрешен, но конкретные значения - implementation dependent. Т.е. нельзя полагаться, что это ID, которые использует OS.

Например такой код:
```cpp
void hello()
{
    cout << "Child: My ID is: " << std::this_thread::get_id() << endl;
}

int main()
{
    std::thread t(hello);

    cout << "Parent: My ID is: " << std::this_thread::get_id() << endl;
    cout << "Parent: Child ID is: " << t.get_id() << endl;

    t.join();

    cout << "Parent: My ID is: " << std::this_thread::get_id() << endl;
    cout << "Parent: Child ID is: " << t.get_id() << endl;

    return 0;
}
```

на моём рабочем MacBook Pro 13'' M1 с операционной системой Mac OS Monterey даёт такой выхлоп:
```
Parent: My ID is: Child: My ID is: 0x100590580
Parent: Child ID is: 0x16fc1f000
0x16fc1f000
Parent: My ID is: 0x100590580
Parent: Child ID is: 0x0
```
