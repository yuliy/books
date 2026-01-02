# Chapter 4. Synchronizing concurrent operations

## 4.1. Waiting for an event or other condition

### 4.1.1. Waiting for a condition with condition variables
The Standard C++ Library provides two implementations of a condition variable:
  * `std::condition_variable` - default and preferred choice. Used with `std::mutex`.
  * `std::condition_variable_any` - more flexible, can work not only with `std::mutex`.

```cpp
std::mutex mut;
std::queue<data_chunk> data_queue;
std::condition_variable data_cond;

void data_preparation_thread()
{
    while (more_data_to_prepare())
    {
        data_chunk const data = prepare_data();
        std::lock_guard<std::mutex> lk(mut);
        data_queue.push(data);
        data_cond.notify_one();
    }
}

void data_processing_thread()
{
    while (true)
    {
        std::unique_lock<std::mutex> lk(mut);
        data_cond.wait(
            lk,
            [] { return !data_queue.empty(); }
        );

        data_chunk data = data_queue.front();
        data_queue.pop();
        lk.unlock();
        process(data);
        if (is_last_chunk(data))
            break;
    }
}
```

Рассмотрим подробнее, что здесь происходит. В потоке `data_preparation_thread()`:
  * Вся работа с очередью `data_queue` и переменной `data_cond` происходит под локом мьютекса.
  * В очередь добавляются новые данные.
  * Переменная нотифицируется. Точнее нотифицируется поток, ожидающий эту переменную.

В потоке `data_processing_thread()`:
  * В цикле создаётся объект `lk`.
  * Далее происходит вызов `data_cond.wait(...)`. Он принимает объект лок типа `std::unique_lock<std::mutex>` и функциональный объект.
  * `data_cond.wait()` разлочивает мьютекс и засыпает до следующего нотифая.
  * Когда в `data_cond` от другого потока приходит нотифай, лок снова лочится и запускается проверка условия (переданный функциональный объект).
    * Если условие возвращает `false`, лок снова разлочивается, а поток снова засыпает в ожидании нотифая.
    * Еслои условие возвращает `true`, лок остаётся залоченным, поток выходит из `data_cond.wait(...)` и продолжает выполнение.
  * Поскольку в это время мьютекс ещё залочен, мы можем без гонки взять данные из очереди.
  * Вот теперь сами вручную разлочиваем мьютекс.
  * И теперь можно долго обрабатывать полученные данные.

Выходит, в этом сценарии у нас есть три сильно связанные сущности:
  * condition variable
  * mutex
  * шаренные данные, которые мы меняем/проверяем под мьютексом

Важно, чтобы функциональный объект, передаваемый в `wait()` был "чистой функцией", т.е. не имел сайд-эффектов. Т.к. в зависимости от реализации могут происходить `spuriuos wakes` (ложные просыпания), т.е. просыпания ожидающего потока без нотифая. В таком случае `wait()` делает как бы холостую проверку условия.

Вообще, вот эта задача передачи данных между потоком-продьюсером в поток-консьюмер через очередь - очень популярная. И её можно свести до одной thread-safe очереди. Об этом ниже.


### 4.1.2. Building a thread-safe queue with condition variables
Хотим примерно такую структуру данных:
```cpp
#include <queue>
#include <mutex>
#include <conditional_variable>

template<typename T>
class threadsafe_queue
{
private:
    std::mutex mut;
    std::queue<T> data_queue;
    std::conditional_variable data_cond;

public:
    void push(T new_value)
    {
        std::lock_guard<std::mutex> lk(mut);
        data_queue.push(new_value);
        data_cond.notify_one();
    }

    void wait_and_pop(T& value)
    {
        std::unique_lock<std::mutex> lk(mut);
        data_cond.wait(lk, [this]{ return !data_queue.empty(); });
        value = data_queue.front();
        data_queue.pop();
    }
};

threadsafe_queue<data_chunk> data_queue;

void data_preparation_thread()
{
    while (more_data_to_prepare())
    {
        data_chunk const data = prepare_data();
        data_queue.push(data);
    }
}

void data_processing_thread()
{
    while (true)
    {
        data_chunk data;
        data_queue.wait_and_pop(data);
        process(data);
        if (is_last_chunk(data))
            break;
    }
}
```

Очевидно, такое решение более user-friendly. Не нужно заботиться о синхронизации. Один поток просто пушит в очередь, другой просто ждёт и попает из очереди.

Полная реализация `threadsafe_queue` выглядит следующим образом:
```cpp
#include <queue>
#include <memory>
#include <mutex>
#include <condition_variable>

template<typename T>
class threadsafe_queue
{
private:
    mutable std::mutex mut;
    std::queue<T> data_queue;
    std::condition_variable data_cond;

public:
    threadsafe_queue() = default;

    threadsafe_queue(threadsafe_queue const& other)
    {
        std::lock_guard<std::mutex> lk(other.mut);
        data_queue = other.data_queue;
    }

    void push(T new_value)
    {
        std::lock_guard<std::mutex> lk(mut);
        data_queue.push(new_value);
        data_cond.notify_one();
    }

    void wait_and_pop(T& value)
    {
        std::unique_lock<std::mutex> lk(mut);
        data_cond.wait(lk, [this]{ return !data_queue.empty(); });
        value = data_queue.front();
        data_queue.pop();
    }

    std::shared_ptr<T> wait_and_pop()
    {
        std::unique_lock<std::mutex> lk(mut);
        data_cond.wait(lk, [this]{ return !data_queue.empty(); });
        std::shared_ptr<T> res(std::make_shared<T>(data_queue.front()));
        data_queue.pop();
        return res;
    }

    bool try_pop(T& value)
    {
        std::lock_guard<std::mutex> lk(mut);
        if (data_queue.empty())
            return false;
        value = data_queue.front();
        data_queue.pop();
        return true;
    }

    std::shared_ptr<T> try_pop()
    {
        std::lock_guard<std::mutex> lk(mut);
        if (data_queue.empty())
            return std::shared_ptr<T>();
        std::shared_ptr<T> res(std::make_shared<T>(data_queue.front()));
        data_queue.pop();
        return res;
    }

    bool empty() const
    {
        std::lock_guard<std::mutex> lk(mut);
        return data_queue.empty();
    }
};
```

В описанных выше примерах нотифай ожидает один поток. Но их может быть сколько угодно. В таком случае при вызове `notify_one()` один из ожидающих потоков получит нотифай. Какой именно - неизвестно, определяется реализацией. Можно также вызвать `notify_all()` - в таком случае все ожидающие потоки получат нотифай.

Задача, когда поток ожидает подготовку данных от другого потока чаще удобнее решать не с помощью condition variable, а с помощью future. Об этом в следующем разделе.


## 4.2. Waiting for one-off events with futures
There are two sorts of futures in the C++ Standard Library:
  * `std::future` - is the one and onlly instance that refers to its associated event.
  * `std::shared_future` - multiple instances of this class may refer to the same event. All the instances will become ready at the same time.

Шаблонный параметр для этих двух типов - это тип данных, с которым ассоциирован фьючерс. То есть данные, которые будут готовы по сигналу. Если никакие ассоциированные данные не планируется использовать, в качестве шаблонного типа следует использовать `void`: `std::future<void>` и `std::shared_future<void>`. Это специализации, которые не передают никакие данные.

Хоть фьючерсы и предназначены для синхронизации потоков, если сам фьючерс шарится между потоками, доступ к нему следует синхронизировать отдельно. Например, с помощью мьютекса. Но если подобная необходимость возникает, имеет смысл рассмотреть использование `std::shared_future`.


### 4.2.1. Returning values from background tasks
Простой пример использования фьючерса:
```cpp
#include <future>
#include <iostream>

int find_the_answer_to_ltuae();
void do_other_stuff();
int main()
{
    std::future<int> the_answer = std::async(find_the_answer_to_ltuae);
    do_other_stuff();
    std::cout << "The answer is " << the_answer.get() << std::endl;
}
```

Функция `std::async` может (также, как и `std::thread`) принимать не только указатель на функцию, но также указатель на метод и аргументы, которые в эти функцию/метод следует передать:
```cpp
struct X
{
    void foo(int, std::string const&);
    std::string bar(std::string const&);
};
X x;

// Calls p->foo(42, "hello") where p is &x
auto f1 = std::async(&X::foo, &x, 42, "hello");

// Calls tmpx.bar("goodbye") where tmpx is a copy of x
auto f2 = std::async(&X::bar, x, "goodbye");

struct Y
{
    double operator()(double);
};
Y y;

// calls tmpy(3.141) where tmpy is move-constructed from Y()
auto f3 = std::async(Y(), 3.141);

// calls y(2.718)
auto f4 = std::async(std::ref(y), 2.718);

X baz(X&);
std::async (baz, std::ref(x)); // calls baz(x)

class move_only
{
    move_only();
    move_only(move_only&&);
    move_only(move_only const&) = delete;
    move_only& operator=(move_only&&);
    move_only& operator=(move_only const&) = delete;

    void operator()();
};

// calls tmp() where tmp is constructed from std::move(move_only())
auto f5 = std::async(move_only());
```

Функция `std::async()` не обязательно выполняет переданный функциональный объект в отдельном треде. На самом деле это определяется политикой выполнения. Дефолтная политика - функция сама выбирает, синхронно выполнять или асинхронно. Но политику можно задать явно:
```cpp
// run in new thread
auto f6 = std::async(std::launch::async, Y(), 1.2);

// run in the same thread when wait() or get() will be called
auto f7 = std::async(std::launch::deferred, baz, std::ref(x));

// implementation dependent how to run
auto f8 = std::async(baz, std::ref(x));

f7.wait(); // invoke deferred function
```

Кроме `std::future` для асинхронного выполнения задач есть ещё:
  * `std::packaged_task`
  * `std::promise`


### 4.2.2. Associating a task with a future
`std::packaged_task` связывает фьючерс с функциональным объектом. Этот класс предназначен для дальнейшей организации пула потоков, обрабатывающих асинхронные таски. Шаблонным параметром класса `std::packaged_task` является сигнатура функции.

Зачем вообще нужен этот `std::packaged_task`? Я понял идею следующим образом:
  * Мы можем обернуть свой фукнциональный объект `std::packaged_task`.
  * Сразу получить фьючерс (метод `std::packaged_task::get_future()`).
  * А далее передать куда-то этот экземпляр `std::packaged_task` на выполнение. Например, в какой-нибудь пул воркеров.

Т.е. если использовать в лоб `std::future` + `std::async`, то мы имеем совершенно конкретный механизм выполнения таска. При этом выполнение запускается одновременно с получением нами фьючерса. А `std::packaged_task` позволяет абстрагировать механизм выполнения, а также сделать его отложенным.

Например, нам нужно обрабатывать таски GUI-треда:
```cpp
#include <deque>
#include <mutex>
#include <future>
#include <thread>
#include <utility>

std::mutex m;
std::deque<std::packaged_task<void()>> tasks;

bool gui_shutdown_message_received();
void get_and_process_gui_message();

void gui_thread()
{
    while (!gui_shutdown_message_received())
    {
        get_and_process_gui_message();
        std::packaged_task<void()> task;
        {
            std::lock_guard<std::mutex> lk(m);
            if (tasks.empty())
                continue;
            task = std::move(tasks.front());
            tasks.pop_front();
        }
        task();
    }
}

std::thread gui_bg_thread(gui_thread);

template<typename Func>
std::future<void> post_task_for_gui_thread(Func f)
{
    std::packaged_task<void()> task(f);
    std::future<void> res = task.get_future();
    std::lock_guard<std::mutex> lk(m);
    tasks.push_back(std::move(task));
    return res;
}
```

What about those tasks that can’t be expressed as a simple function call or those tasks where the result may come from more than one place? These cases are dealt with by the third way of creating a future: using a `std::promise` to set the value explicitly.


### 4.2.3. Making (std::)promises
`std::promise` - это объект, позволяющий предоставить данные, ожидаемые каким-нибудь фьючерсом. Т.е. создаётся и используется всегда пара: `std::promise` и `std::future`.

Рассмотрим пример, в котором потоки обмениваются простыми булевыми флагами, которые сообщают об успешности передачи пакетов данных:
```cpp
#include <future>

void process_connections(connection_set& connections)
{
    while (!done(connections))
    {
        for (
            auto connection = connections.begin(), end = connections.end();
            connection != end;
            ++connection
        )
        {
            if (connection->has_incoming_data())
            {
                data_packet data = connection->incoming();
                std::promise<payload_type>& p = connection->get_promise(data.id);
                p.set_value(data.payload);
            }

            if (connection->has_outgoing_data())
            {
                outgoing_packet data = connection->top_of_outgoing_queue();
                connection->send(data.payload);
                data.promise.set_value(true);
            }
        }
    }
}
```


### 4.2.4. Saving an exception for the future
Все описанные выше сущности прозрачны с т.зр. исключений. Т.е. есть исходная функция кидает исключение, то `std::future::get()` либо вернёт данные, либо кинет исключение. Причём это может быть копией исходного исключение, а не оригинальный объект (зависит от реализации, стандарт оставляет здесь свободу).

При использовании `std::promise` соответственно можно вернуть значение, а можно кинуть исключение:
```cpp
extern std::promise<double> some_promise;

try
{
    some_promise.set_value(calculate_value());
}
catch (...)
{
    some_promise.set_exception(std::current_exception());
}
```


### 4.2.5. Waiting from multiple threads
`std::future` предназначен для синхронизации двух потоков: один производит данные, другой получает. Если несколько потоков попытаются получить данных (`std::future::get()`), будет data race. Их обращение к фьючерсу необходимо синхронизировать отдельно. Но это не имеет смысла, т.к. `std::future` спроектирован для эксклюзивного получения данных одним потоком. Если же данные должны получать несколько потоков, следует использовать `std::shared_future`.

`std::future` - movable, а `std::shared_future` - copyable. Шаред создаётся мувом обычного фьючерса:
```cpp
std::promise<int> p;
std::future<int> f(p.get_future());
assert(f.valid());
std::shared_future<int> sf(std::move(f));
assert(!f.valid());
assert(sf.valid());
```

Можно также написать короче:
```cpp
std::promise<int> p;
std::shared_future<int> sf(p.get_future());
```

А можно ещё короче вот так:
```cpp
std::promise<int> p;
auto sf = p.get_future().share();
```


## 4.3. Waiting with a time limit
БОльшая часть примитивов синхронизации имеют также методы для ожидания с таймаутом. Все такие методы имеют два вариантов суффиксов в названии:
  * `_for()` - ожидание заданного промежутка времени (`std::chrono::duration`)
  * `_until()` - ожидание до конкретного абсолютного значения времени (`std::chrono::time_point`)

Некоторые примитивы не имеют таких методов, например, `std::mutex`. Но есть соответствующие "дублирующие" примитивы, предоставляющие такую функциональность, например, `std::timed_mutex`.

Подробнее см. таблицу ниже:
![](/img/table_4_1.png)

## 4.4. Using synchronization of operations to simplify code

### 4.4.1. Functional programming with futures
Фьючерсы в паря с лямбда-выражениями облегчают программирование в функциональном стиле. Не нужно думать про синхронизацию на cond var: кого когда как залочить. Просто описываем "чистые функции" и получаем результат через фьючерсы.

Вместо использования `std::async` можно сделать собственную обёртку. Например таким образом:
```cpp
template<typename F, typename A>
std::future<std::result_of<F(A&&)>::type> spawn_task(F&&, A&& a)
{
    using result_type = std::result_of<F(A&&)>::type;
    std::packaged_gask<result_type(A&&)> task(std::move(f));
    std::future<result_type> res(task.get_future());
    std::thread t(std::move(task), std::move(a));
    t.detach();
    return res;
}
```

### 4.4.2. Synchronizing operations with message passing
Кроме функционального программирования, есть другие парадигмы написания распраллеленного кода. Например, CSP = Communicating Sequential Processes.

В CSP потоки принципиальн не шарят никакие данные. Они обмениваются сообщениями через каналы. Эта парадигма реализована например в языке Erlang.

В таком подходе потоки не занимаются синхронизация общения друг с другом и синхронизацией обращения к шаренным данным. Они получают на входе сообщения и обрабатывают их как стейт-машина. При этом сами могут отправлять сообщения другим потокам.
