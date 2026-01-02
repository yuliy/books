#include <iostream>
#include <thread>
using namespace std;

int main()
{
    cout << "std::thread::hardware_concurrency(): "
        << std::thread::hardware_concurrency()
        << endl;
    return 0;
}

