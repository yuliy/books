# Chapter 1. Hello, world of concurrency in C++!

В этой книге "concurrency" определяют как "две или более активностей происходящих одновременно". Мне все-таки больше нравятся опредления, которые давал Рома Липовский в своих лекциях:
  * **parallelism** - Нечто происходящее параллельно/одновременно.
  * **concurrency** - Ситуация одновременного использования двумя или более "активностями" одного (разделяемого ресурса).

При этом concurrency может существовать без parallelism. Например на одноядерном процессоре никакого параллелизма нет.

Concurrent hello world program:
```cpp
#include <iostream>
#include <thread>
using namespace std;

void hello()
{
    cout << "Hello from another thread!" << endl;
}

int main()
{
    std::thread t(hello);
    t.join();
    return 0;
}
```
