#include <iostream>
#include <thread>
using namespace std;

void hello()
{
    cout << "Hello from another thread!" << endl;:wchar_t
}

int main()
{
    std::thread t(hello);
    t.join();
    return 0;
}

