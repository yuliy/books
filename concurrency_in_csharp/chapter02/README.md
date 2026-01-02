[<<<](../README.md)


# Chapter 2. Async Basics
This chapter is about using `async` and `await` for asynchronous operations.


## 2.1. Pausing for a Period of Time

### Problem
We need to wait (asynchronously) for a period of time. May be useful for timeouts.

### Solution
This example returns a task used for the asynchronous success case:
```csharp
static async Task<T> DelayResult<T>(T result, TimeSpan delay)
{
    await Task.Delay(delay);
    return result;
}
```

Simple implementation of exponential backoff:
```csharp
static async Task<string> DownloadStringWithRetries(string uri)
{
    using (var client = new HttpClient())
    {
        var nextDelay = TimeSpan.FromSeconds(1);
        for (int i = 0; i != 3; ++i)
        {
            try
            {
                return await client.GetStringAsync(uri);
            }
            catch
            {
            }

            await Task.Delay(nextDelay);
            nextDelay += nextDelay;
        }

        // Try one last time, allowing the error to propagate
        return await client.GetStringAsync(uri);
    }
}
```

> It's definitely not a production solution. It is better to use e.g. Transient Error Handling Block from Microsoft's Enterprise Library.

Final example uses `Task.Delay` as a simple timeout:
```csharp
static async Task<string> DownloadStringWithTimeout(string url)
{
    using (var client = new HttpClient())
    {
        var downloadTask = client.GetStringAsync(uri);
        var timeoutTask = Task.Delay(3000);

        var completedTask = await Task.WhenAny(downloadTask, timeoutTask);
        if (completedTask == timeoutTask)
            return null;
        return await downloadTask;
    }
}
```

### Discussion
`Task.Delay` is a fine option for unit testing asynchronous code or for retry logic. But for timeouts `CancellationToken` is usually a better choice.


## 2.2. Returning Completed Tasks

### Problem
You need asynchronous interface with synchronous implementation. E.g. when unit testing asynchronous code.

### Solution
You can use `Task.FromResult` to create and return a new `Task<T>` that is already completed with the specified value:
```csharp
interface IMyAsyncInterface
{
    Task<int> GetValueAsync();
}

class MySynchronousImplementation : IMyAsyncInterface
{
    public Task<int> GetValueAsync()
    {
        return Task.FromResult(13);
    }
}
```

### Discussion
If you are implementing an asynchronous interface with synchronous code, avoid any form of blocking. Otherwise, if an asynchronous method blocks, it prevents the calling thread from starting other tasks, which interferes with concurrency and may even cause a deadlock.

E.g. `Console.In.ReadLineAsync` will actually block the calling thread until a line is read, and then will return a completed task. Actually it looks like a bug in frmework. E.g. [see this article](https://smellegantcode.wordpress.com/2012/08/28/a-boring-discovery/). It explains that the implementation is as follows:
```csharp
public override Task<string> ReadLineAsync()
{
    return Task.FromResult<string>(this.ReadLine());
}
```

Looks really dull... This article also provides a workaround:
```csharp
public static Task<string> ReadConsoleAsync()
{
    return Task.Run(() => Console.ReadLine());
}
```

As for me, it look pretty obvious, but I'm not sure that it is a best solution.

This article was written in 2012 (12 years ago). So, I also looked through [the microsoft doc page about StreamReader.ReadLineAsync](https://learn.microsoft.com/en-us/dotnet/api/system.io.streamreader.readlineasync?view=net-9.0) and it looks like this problem is already fixed.

`Task.FromResult` allows us with successfull results only. If you need a task with a different kind of result, you can do as follows:
```csharp
static Task<T> NotImplementedAsync<T>()
{
    var tcs = new TaskCompletionSource<T>();
    tcs.SetException(new NotImplementedException());
    return tcs.Task;
}
```

Conceptually, `Task.FromResult` is a shorthand for using `TaskCompletionSource`.

If you regularly use `Task.FromResult` with the same value, consider caching the task. E.g.:
```csharp
private static readonly Task<int> zeroTask = Task.FromResult(0);

static Task<int> GetValueAsync()
{
    return zeroTask;
}
```


## 2.3. Reporting Progress

### Problem
You need to respond to progress while an asynchronous operation is executing.

### Solution
Use the provided `IProgress<T>` and `Progress<T>` types:
```csharp
static async Task MyMethodAsync(IProgress<double> progress = null)
{
    double percentComplete = 0;
    while (!done)
    {
        ...
        if (progress != null)
            progress.Report(percentComplete);
    }
}

static async Task CallMyMethodAsync()
{
    var progress = new Progress<double>();
    progress.ProgressChanged += (sender, args) =>
        {
            ...
        };
    await MyMethodAsync(progress);
}
```

### Discussion
Bear in mind that the `IProgress<T>.Report` method may by asynchronous. This means that `MyMethodAsync` may continue executing before the progress is actually reported. For this reason, it's best to define `T` as an `immutable type` or at least a value type. If `T` is a mutable reference type, then you'll have to create a separate copy yourself each time you call `IProgress<T>.Report`.


## 2.4. Waiting for a Set of Tasks to Complete

Use `Task.WhenAll`:
```csharp
Task task1 = Task.Delay(TimeSpan.FromSeconds(1));
Task task2 = Task.Delay(TimeSpan.FromSeconds(2));
Task task3 = Task.Delay(TimeSpan.FromSeconds(1));

await Task.WhenAll(task1, task2, task3);
```

There is also an overload that takes `IEnumerable` of tasks:
```csharp
static async Task<string> DownloadAllAsync(IEnumerable<string> urls)
{
    var httpClient = new HttpClient();
    var downloads = urls.Select(url => httpClient.GetStringAsync(url));
    Task<string>[] downloadTasks = downloads.ToArray();
    string[] htmlPages = await Task.WhenAll(downloadTasks);
    return string.Concat(htmlPages);
}
```

If any of these tasks throws an exception, `Task.WhenAll` will fail with exception. If multiple tasks fail, only one of these exceptions will be thrown by `Task.WhenAll`. However, you can get all the exception from `Task.Exception`:
```csharp
static async Task ThrowNotImplementedExceptionAsync()
{
    throw new NotImplementedException();
}

static async Task ThrowInvalidOperationExceptionAsync()
{
    throw new InvalidOperationException();
}

static async Task ObserveAllExceptionsAsync()
{
    var task1 = ThrowNotImplementedExceptionAsync();
    var task2 = ThrowInvalidOperationExceptionAsync();

    Task allTasks = Task.WhenAll(task1, task2);
    try
    {
        await allTasks;
    }
    catch (Exception ex)
    {
        // "ex" is either NotImplementedException or InvalidOperationException
        AggregationException allExceptions = allTasks.Exception;
        // ...
    }
}
```


## 2.5. Waiting for Any Task to Complete

Use the `Task.WhenAny` method. E.g.:
```csharp
private static async Task<int> FirstRespondingUrlAsync(string urlA, string urlB)
{
    var httpClient = new HttpClient();

    Task<byte[]> downloadTaskA = httpClient.GetByteArrayAsync(urlA);
    Task<byte[]> downloadTaskB = httpClient.GetByteArrayAsync(urlB);

    Task<byte[]> completedTask = await Task.WhenAny(
        downloadTaskA,
        downloadTaskB
    );

    bytes[] data = await completedTask;
    return data.Length;
}
```

The task returned by `Task.WhenAny` never completes in a faulted or canceled state. It always results in the first `Task` to complete; if that task completed with an exception, then the exception is not propagated to the task returned by `Task.WhenAny`. For this reason, you should usually await the task after it has completed.

Using `Task.WhenAny` for iterating completed task is an antipatter. Because every time task is completed, you should delete it and run `Task.WhenAny`. Hence, it task `O(N^2)` to be executed.


## 2.6. Processing Tasks as They Complete
There are a few different approaches to solve this problem. Here's the recommended one:
```csharp
// This is our business logic
static async Task<int> DelayAndReturnAsync(int val)
{
    await Task.Delay(TimeSpan.FromSeconds(val));
    return val;
}

// And this method is helper/wrapper
static async Task AwaitAndProcessAsync(Task<int> task)
{
    var result = await task;
    Trace.WriteLine(result);
}

static async Task ProcessTaskAsync()
{
    Task<int> taskA = DelayAndReturnAsync(2);
    Task<int> taskB = DelayAndReturnAsync(3);
    Task<int> taskC = DelayAndReturnAsync(1);
    var tasks = new[] { taskA, taskB, taskC };

    var processingTasks = (from t in tasks
        select AwaitAndProcessAsync(t)).ToArray();

    await Task.WhenAll(processingTasks);
}
```

Alternatively, this can be written as:
```csharp
static async Task<int> DelayAndReturnAsync(int val)
{
    await Task.Delay(TimeSpan.FromSeconds(val));
    return val;
}

static async Task ProcessTasksAsync()
{
    Task<int> taskA = DelayAndReturnAsync(2);
    Task<int> taskB = DelayAndReturnAsync(3);
    Task<int> taskC = DelayAndReturnAsync(1);
    var tasks = new[] { taskA, taskB, taskC };

    var processingTasks = tasks.Select(async t =>
        {
            var result = await t;
            Trace.WriteLine(result);
        }
    ).ToArray();

    await Task.WhenAll(processingTasks);
}
```

This refactoring is the cleanest and most portable way to solve this problem. However, it is subtly different than the original code. This solution will do the task processing concurrently, whereas the original code would do the task processing one at a time. Most of the time this is not a problem, but if it is not acceptable for your situation, then consider using locks or the following alternative solution.

One more alternative is an extension method `Task.OrderByCompletion` developed by Stephen Toub and Jon Skeet:
```csharp
static async Task<int> DelayAndReturnAsync(int val)
{
    await Task.Delay(TimeSpan.FromSeconds(val));
    return val;
}

static async Task ProcessTasksAsync()
{
    Task<int> taskA = DelayAndReturnAsync(2);
    Task<int> taskB = DelayAndReturnAsync(3);
    Task<int> taskC = DelayAndReturnAsync(1);
    var tasks = new[] { taskA, taskB, taskC };

    foreach (var task in tasks.OrderByCompletion())
    {
        var result = await task;
        Trace.WriteLine(result);
    }
}
```


## 2.7. Avoiding Context for Continuations
Use `ConfigureAwait(false)` to avoid resuming on a context:
```csharp
async Task ResumeWithoutContextAsync()
{
    await Task.Delay(TimeSpan.FromSeconds(1)).ConfigureAwait(false);
}
```

The question is, HOW MANY continuations on the UI thread is TOO MANY? There is no hard-and-fast answer, but Lucian Wischik of Microsoft has publicized the guideline used by the WinRT team: a hundred or so per second is OK, but a thousand or so per second is too many.

It's best to avoid this right at the beginning.


## 2.8. Handling Exceptions from async Task Methods
This topic was covered in Chapter 1. Key ideas:
  * Exception handling is straightforward.
  * Exception is thrown only when the task is awaited.
  * When you call `await`, initial exception is rethrown. The original stack trace is correctly preserved.


## 2.9. Handling Exceptions from async Void Methods

### Problem
You have an async void method and need to handle exceptions propagated out of that method.

### Solution
There is no good solution. It at all possible, change the method to return `Task` instead of `void`. In some situations, this isn't possible; e.g. let's say you need to unit test an `ICommand` implementation (which must return `void`). In this case, you can provide a `Task`-returning overload of your `Execute` method as such:
```csharp
sealed class MyAsyncCommand : ICommand
{
    async void ICommand.Execute(object parameter)
    {
        await Execute(parameter);
    }

    public async Task Execute(object parameter)
    {
        // ... Asynchronous command implementation goes here.
    }

    // ...
}
```

It's best to avoid propagating exceptions out of `async void` methods. If you must use an `async void` method, consider wrapping all of its code in a `try` block and handling the exception directly.

There are also some other solutions.

