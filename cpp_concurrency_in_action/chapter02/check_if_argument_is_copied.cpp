#include <iostream>
#include <thread>
using namespace std;

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

