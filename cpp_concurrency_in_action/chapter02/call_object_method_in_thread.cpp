#include <iostream>
#include <thread>
using namespace std;

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

