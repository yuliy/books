#include <iostream>
#include <thread>
using namespace std;

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

