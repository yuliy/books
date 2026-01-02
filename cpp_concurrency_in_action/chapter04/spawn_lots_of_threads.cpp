#include <atomic>
#include <chrono>
#include <future>
#include <iostream>
#include <thread>
#include <vector>
using namespace std;

constexpr int THREADS_TO_SPAWN = 1000 * 1000;
std::atomic<int> cnt = 0;

class SteadyTimer
{
public:
    SteadyTimer() { Reset(); }
    void Reset() { start_ = std::chrono::steady_clock::now(); }
    double TimeElapsedInSeconds() const
    {
        const auto now = std::chrono::steady_clock::now();
        const auto elapsed_ms = std::chrono::duration_cast<std::chrono::microseconds>(now - start_).count();
        return 1e-6 * elapsed_ms;
    }

private:
    std::chrono::steady_clock::time_point start_;
};

int thread_func()
{
    const int my_num = cnt++;
    //cout << "Starting thread #" << my_num << " ..." << endl;
    std::this_thread::sleep_for(10ms);
    //cout << "Finishing thread #" << my_num << " ..." << endl;
    return my_num;
}

int main()
{
    cout << "THREADS_TO_SPAWN=" << THREADS_TO_SPAWN << endl;

    SteadyTimer timer;
    std::vector<std::future<int>> futures;
    for (int i = 0; i < THREADS_TO_SPAWN; ++i)
        futures.push_back(std::async(thread_func));
    cout << "Time to create threads: " << timer.TimeElapsedInSeconds() << endl;

    timer.Reset();
    for (auto& fut : futures)
    {
        //cout << "Thread #" << fut.get() << " finished." << endl;
        fut.get();
    }
    cout << "Time to wait for threads: " << timer.TimeElapsedInSeconds() << endl;
    cout << "Done." << endl;
    return 0;
}

