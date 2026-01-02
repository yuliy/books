[<<<](../README.md)


# Chapter 1. Concurrency: An Overview

## 1.1. Introduction to Concurrency

> **Concurrency** is doing more than one thing at a time.

> **Multithreading** is a form of concurrency that uses multiple threads of execution.

> **Parallel Processing** is doing lots of work by dividing it up among multiple threads that run concurrently.

> **Asynchronous Programming** is a form of concurrency that uses futures or callbacks to avoid unnecessary threads.

Modern future types in .NET are `Task` and `Task<Result>`. Older asynchrounous APIs use callbacks or events instead futures. Asynchronous programming is centered around the idea of an **asynchronous operation**: some operation is started that will complete some time later. While the operation is in progress, it does not block the original thread; the thread that starts the operation is free to do other work. When the operation completes, it notifies its future or invokes its completion callback event.

Asynchronous programming is a powerful form of concurrency, but until recently, it required extremely complex code. The `async` and `await` support in VS2012 make asynchronous programming almost as easy as synchronous (nonconcurrent) programming.

Another form of concurrency is **reactive programming**. Reactive programming is closely related to asynchronous programming, but is built on **asynchronous events** instead **asynchronous operations**. Asynchronous events may not have an actual "start", may happen at any time, and may be raised multiple times.

> **Reactive Programming** is a declarative style of programming where the application reacts to events.

Reactive programming is not necessarily concurrent, but it is closely related to concurrency.


## 1.2. Introduction to Asynchronous Programming
Two primary benefits of asynchronous programming:
  1. For end-user GUI programs: asynchronous programming enables responsiveness.
  2. For server-side programs: asynchronous programming enables scalability.
     <br>A server application can scale somewhat just by using the thread pool, but an asynchronous server application can usually scale an order of magnitude better than that.

Modern async .NET apps use two keywords: `async` and `await`. The `async` keyword is added to a method declaration, and its primary purpose is to enable the `await` keyword within that method. An `async` method should return `Task<T>` or `Task`.

Example:
```csharp
async Task DoSomethingAsync()
{
    int val = 13;

    // Asynchrounously wait 1 second.
    await Task.Delay(TimeSpan.FromSeconds(1));

    val *= 2;

    // Asynchronously wait 1 second.
    await Task.Delay(TimeSpan.FromSeconds(1));

    Trace.WriteLine(val);
}
```

`await` keyword performs an `asynchronous wait` on its argument. First, it checks whether the operation is already complete; if it is, it continues executing (synchronously). Otherwise, it will pause the `async` method and return incomplete task. When the operation completes some time later, the `async` method will resume executing.

You can think of an `async` method as having several synchronous portions, broken up by `await` statements. The first synchronous portion executes on whatever thread calls the method, but where to the other synchronous portions execute? The answer is a bit complicated.

When you `await` a task, a `context` is captured when the `await` decides to pause the method. This context is the current `SynchronizationContext` unless it is null, in which case the context is the current `TaskScheduler`. The method resumes executing within the captured context. Usually, this context is the UI context, an ASP.NET request context, or the thread pool context (most other situations).

So, the default behaviour is using the same context. But these behaviour may be changed via calling `ConfigureAwait`:
```csharp
async Task DoSomethingAsync()
{
    int val = 13;

    // Asynchrounously wait 1 second.
    await Task.Delay(TimeSpan.FromSeconds(1)).ConfigureAwait(false);

    val *= 2;

    // Asynchronously wait 1 second.
    await Task.Delay(TimeSpan.FromSeconds(1)).ConfigureAwait(false);

    Trace.WriteLine(val);
}
```

If you call `DoSomethingAsync` from a UI thread, each of its synchronous portions will run on that UI thread; but if you call it from a thread-pool thread, each of its synchronous portions will run on a thread-pool thread.

Actually the default behaviour is mostly used in UI. Hence good practice is to always call `ConfigureAwait(false)` in core "library" methods.

There are 2 basic ways to create a `Task`:
  * calling `Task.Run()` (or `TaskFactory.StartNew()`) - for computational tasks
  * `TaskCompletionSource<T>` (or some of its shortcuts) - tasks representing notification

Exception handling with `async` and `await` is natural:
```csharp
async Task TrySomethingAsync()
{
    try
    {
        await PossibleExceptionAsync();
    }
    catch (NotSupportedException ex)
    {
        LogException(ex);
        throw;
    }
}
```

**!WARNING!** Resist the temptation of calling `Task.Wait` or `Task<T>.Result` manually; this could cause a deadlock. Consider this method:
```csharp
async Task WaitAsync()
{
    // This await will capture the current context ...
    await Task.Delay(TimeSpan.FromSeconds(1));
    // ... and will attempt to resume the method here in that context.
}

void Deadlock()
{
    // Start the delay.
    Task task = WaitAsync();

    // Synchronously block, waiting for the async method to complete.
    task.Wait();
}
```

This code will deadlock if called from a UI or ASP.NET context. This is because both of those contexts only allow one thread in at a time.

More info about async in C#:
  * ["Async in C# 5.0" by Alex Davies](https://www.oreilly.com/library/view/async-in-c/9781449337155/)
  * Microsoft documentation about async:
    * [Async Overview](https://learn.microsoft.com/en-us/previous-versions/hh191443(v=vs.140))
    * [Task-based Asynchronous Pattern (TAP)](https://learn.microsoft.com/en-us/dotnet/standard/asynchronous-programming-patterns/task-based-asynchronous-pattern-tap?redirectedfrom=MSDN)
  * etc


## 1.3. Introduction to Parallel Programming
There are two forms of parallelism:
  * data parallelism - when you have a bunch of data items to process, and the processing of each piece of data is mostly independent from the other pieces
  * task parallelism - is when you have a pool of work to do, and each piece of work is mostly independent from the other pieces

There are a few different ways to do data parallelism:
  * `Parallel.ForEach` & `Parallel.For`
    ```csharp
    void RotateMatrices(IEnumerable<Matrix> matrices, float degrees)
    {
        Parallel.ForEach(matrices, matrix => matrix.Rotate(degrees));
    }
    ```
  * PLINQ (Parallel LINQ)
    ```csharp
    IEnumerable<bool> PrimalityTest(IEnumerable<int> values)
    {
        return values.AsParallel().Select(val => IsPrime(val));
    }
    ```

`Parallel` provides functionality for task parallelism either, e.g.:
```csharp
void ProcessArray(double[] array)
{
    Parallel.Invoke(
        () => ProcessPartialArray(array, 0, array.Length / 2),
        () => ProcessPartialArray(array, array.Length / 2, array.Length)
    );
}

void ProcessPartialArray(double[] array, int begin, int end)
{
    // CPU-intensive processing ...
}
```

`Task` type is a future. Hence, it may be used directly. But nowadays there highg-level abstractions like `Parellel`. These abstractions provide dynamic parallelism - when you don't know how many pieces of work you need to do at the beginning.

Since operations are proceeding in parallel, it is possible for multiple exceptions to occure, so they are wrapped up in an `AggregateException`:
```csharp
try
{
    Parallel.Invoke(
        () => { throw new Exception(); },
        () => { throw new Exception(); }
    );
}
catch (AggregationException ex)
{
    ex.Handle(exception =>
        {
            Trace.WriteLine(exception);
            return true; // "handled"
        }
    );
}
```

Further reading on parallel programming:
  * [Parallel Programming with Microsoft .NET, by Colin Campbell](https://learn.microsoft.com/en-us/previous-versions/msp-n-p/ff963553(v=pandp.10))


## 1.4. Introduction to Reactive Programming (Rx)
Reactive programming is based around the notion of observable streams. When you subscribe to an observable stream, you'll receive any number of data items (`OnNext`) and then the stream may end with a single error (`OnError`) or "end of stream" notification (`OnCompleted`).

```csharp
interface IObserver<in T>
{
    void OnNext(T item);
    void OnCompleted();
    void OnError(Exception error);
}

interface IObservable<out T>
{
    IDisposable Subscribe(IObserver<T> observer);
}
```

However, you should never implement these interfaces. The Reactive Extensions (Rx) library by Microsoft has all the implementations you should ever need. Reactive code ends up looking very much like LINQ; you can think of it as "LINQ to events".

E.g.:
```csharp
Observable.Interval(TimeSpan.FromSeconds(1)) # counter running a periodic timer
    .Timestamp()                            # add timestamp to each event
    .Where(x => x.Value % 2 == 0)           # filter the events
    .Select(x => x.Timestamp)               # select the timestamp values
    .Subscribe(x => Trace.WriteLine(x));    # here x is a timestamp
```

Rx is very similar to LINQ. The main difference is that LINQ to Objectx and LINQ to Entities is a "pull" model, where the enumeration of a LINQ query pulls the data through the query, whlie LINQ to events (Rx) uses a "push" model, where the events arrive and travel through the query themselves.

One more example:
```csharp
IObservable<DateTimeOffset> timestamps =
    Observable.Interval(TimeSpan.FromSeconds(1))
        .Timestamp()
        .Where(x => x.Value % 2 == 0)
        .Select(x => x.Timestamp);

timestamps.Subscribe(x => trace.WriteLine(x));
```

It is normal for a type to define the observable streams and make the available as an `IObservable<T>` resource. Other types can then subscribe to those streams or combine them with other operators to create another observable stream.

Actually error handling should be provided always. E.g.:
```csharp
Observable.Interval(TimeSpan.FromSeconds(1))
    .Timestamp()
    .Where(x => x.Value % 2 == 0)
    .Select(x => x.Timestamp)
    .Subscribe(
        x => Trace.WriteLine(x),    // OnNext
        ex => Trace.WriteLine(x)    // OnError
    );
```

Further reading on reactive programming in .NET:
  * [Introduction to Rx](https://introtorx.com/) - online book


## 1.5. Introduction to Dataflows
TPL (Task Parallel Library) Dataflow is a library for making pipelines on dataflows. The basic building unit of a dataflow mesh is a **dataflow block**. Block can either be target black (receiving data), a source block (producing data), or both. Source blocks can be linked to target blocks. Blocks are semi-independent.

!IMPORTANT! Target blocks have buffers for the data the receive. This allows the to accept new data items even if they are not ready to process yet, keeping data flowing through the mesh. This buffering ca cause problems in fork scenarios, where one source block is linked to two target blocks. When source block has data to send downstream, it starts offering it to its linked blocks one at a time. By default, the first target block would just take the data and buffer it, and the second target block would never get any. The fix for this situation is to limit thetarget block buffers by making them nongreedy.

I think the previous note is important for implementation of `GraphHost` graph. I have an idea to implement graph flow with TPL Dataflows.

TPL Dataflows vs Rx:
  * Rx observables are generally better when doing anything related to timing.
  * Dataflow blocks are generally better when doing parallel processing.
  * Conceptually, Rx works more like setting up callbacks: each step in observable directly calls the next step.
  * In contrast, each block in a dataflow mesh is very independent from all the other blocks.
  * Rx and TPL Dataflow have their own uses, with some amount of overlap. However, they also work quite well together.

Further reading:
  * [Microsoft: Dataflow (Task Parallel Library)](https://learn.microsoft.com/en-us/dotnet/standard/parallel-programming/dataflow-task-parallel-library)
  * [Microsoft: Guide to Implementing Custom TPL Dataflow Blocks](https://download.microsoft.com/download/1/6/1/1615555D-287C-4159-8491-8E5644C43CBA/Guide%20to%20Implementing%20Custom%20TPL%20Dataflow%20Blocks.pdf)


## 1.6. Introduction to Multithreaded Programming
There is almost no need to ever create a new thread yourself.


## 1.7. Collections for Concurrent Applications
**Concurrent collections** allow multiple threads to update them simultaneously in a safe way. Most concurrent collections use snapshots. Concurrent collections are usually more efficient than just protecting a regular collection with a lock.

**Immutable collections** cannot be modified. To modify, you create a new collection that represents the modified collection. Sounds horribly inefficient, but immutable collections share as much memory as possible.


## 1.8. Modern Design
Most concurrent technologies are functional in nature. Key principles of functional programming are:
  * Purity. That is, avoiding side effects.
  * Immutability. That is, a piece of data cannot change.


## 1.9. Summary of Key Technologies
`async` and `await` keywords were introduced in .NET 4.5 in 2012.

The Task Parallel Library was introduced in .NET 4.0.

Interesting fact that:
  * [Rx-Main NuGet Package](https://www.nuget.org/packages/Rx-Main) is deprecated at the moment of writing this conspect (december 2024).
  * [Microsoft.Tpl.Dataflow Package](https://www.nuget.org/packages/Microsoft.Tpl.Dataflow) is deprecated either.
  * [Microsoft.Bcl.Immutable Package](https://www.nuget.org/packages/Microsoft.Bcl.Immutable) is deprecated either.

