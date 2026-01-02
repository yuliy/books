#include <vector>
#include <cstdlib>
#include <iostream>
#include <ctime>
#include <chrono>
#include <string>

using namespace std;
using yclock = std::chrono::high_resolution_clock;

vector<int> GenerateRandomArray(int size) {
    vector<int> res;
    for (int i= 0; i < size; ++i) {
        const int elem = rand() % 2000000 - 1000000;
        res.push_back(elem);
    }
    return res;
}

bool CheckSorted(const vector<int>& arr) {
    for (int i = 1; i < arr.size(); ++i) {
        if (arr[i] < arr[i-1])
            return false;
    }
    return true;
}

void printTimeCheckPoint(yclock::time_point start, string msg) {
    auto end = yclock::now();
    auto duration = chrono::duration_cast<chrono::microseconds>(end - start).count();
    cout << msg << (duration / 1000.0) << " ms" << endl;
}

int main() {
    cout << endl << "<<< C++ >>>" << endl;
    srand (time(NULL));
    const int size = 100 * 1000 * 1000;
    cout << "Size: " << size << endl;

    //
    auto start = yclock::now();
    vector<int> arr = GenerateRandomArray(size);
    printTimeCheckPoint(start, "Random array generated in ");

    //
    start = yclock::now();
    sort(arr.begin(), arr.end());
    printTimeCheckPoint(start, "Array sorted in ");

    //
    start = yclock::now();
    cout << "sorted: " << (CheckSorted(arr) ? "true" : "false") << endl;
    printTimeCheckPoint(start, "Sort correctness is checked in ");

    return 0;
}
