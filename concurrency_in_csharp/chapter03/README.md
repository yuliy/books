[<<<](../README.md)


# Chapter 3. Parallel Basics
Use parallel programming for CPU-bound work. Use asynchronous programming for I/O-bound work.

The parallel processing abstractions covered in this chapter are part of the Task Parallel Library (TPL). It is built in to the .NET framework.


## 3.1. Parallel Processing of Data

### Problem
You have a collection of data and you need to perform the same operation on each element of the data. This operation is CPU-bound and may take some time.

### Solution
```csharp
void RotateMatrices(IEnumerable<Matrix> matrices, float degrees)
{
    Parallel.ForEach(matrices, matrix => matrix.Rotate(degrees));
}
```

If you want to stop the loop early:
```csharp
void InvertMatrices(IEnumerable<Matrix> matrices)
{
    Parallel.ForEach(
        matrices,
        (matrix, state) =>
        {
            if (!matrix.IsInvertible)
                state.Stop();
            else
                matrix.Invert();
        }
    );
}
```

In code earlier the loop is stopped inside the loop. In case you to stop in outside (i.e. cancel the loop):
```csharp
void RotateMatrices(IEnumerable<Matrix> matrices, float degrees,
    CancellationToken token)
{
    Parallel.ForEach(
        matrices,
        new ParallelOptions { CancellationToken = token },
        matrix => matrix.Rotate(degrees)
    );
}
```

Each parallel task may run on a different thread, so any shared state must be protected:
```csharp
int InvertMatrices(IEnumerable<Matrix> matrices)
{
    object mutex = new object();
    int nonInvertibleCount = 0;
    Parallel.ForEach(
        matrices,
        matrix =>
        {
            if (matrix.IsInvertible)
            {
                matrix.Invert();
            }
            else
            {
                lock (mutex)
                {
                    ++nonInvertibleCount;
                }
            }
        }
    );
}
```


## 3.2. Parallel Aggregation

### Problem
At the conclusion of a parallel operation, you have to aggregate the results. Examples of aggregation are sums, averages, etc.

### Solution
The `Parallel` supports aggregation through the concept of `local values`, which are variables that exist locally within a parallel loop:
```csharp
static int ParallelSum(IEnumerable<int> values)
{
    object mutex = new object();
    int result = 0;
    Parallel.ForEach(
        source: values,
        localInit: () => 0,
        body: (item, state, localValue) => localValue + item,
        localFinally: localValue =>
            {
                lock (mutex)
                    result += localValue;
            }
    );

    return result;
}
```

Parallel LINQ has more natural aggregation support than the `Parallel` class:
```csharp
static int ParallelSum(IEnumerable<int> values)
{
    return values.AsParallel().Sum();
}
```

Ok, sum is cheat in case of PLINQ, because it has built-in `Sum()` operation. It has some more built-in operations. But here's a more generic implementation:
```csharp
static int ParallelSum(IEnumerable<int> values)
{
    return values.AsParallel().Aggregate(
        seed: 0,
        func: (sum, item) => sum + item
    );
}
```


## 3.3. Parallel Invocation

### Problem
You have a number of methods to call in parallel, and these methods are (mostly) independent of each other.

### Solution
```csharp
static void ProcessArray(double[] array)
{
    Parallel.Invoke(
        () => ProcessPartialArray(array, 0, array.Length / 2),
        () => ProcessPartialArray(array, array.Length / 2, array.Length)
    );
}

static void ProcessPartialArray(double[] array, int begin, int end)
{
    // CPU-intensive processing ...
}
```

You can also pass an array of delegates to the Parallel.Invoke method if the number of invocations is not known until runtime:
```csharp
static void DoAction20Times(Action action)
{
    Action[] actions = Enumerable.Repeat(action, 20).ToArray();
    Parallel.Invoke(actions);
}
```

`Parallel.Invoke` supports cancellation:
```csharp
static void DoAction20Times(Action action, CancellationToken token)
{
    Action[] actions = Enumerable.Repeat(action, 20).ToArray();
    Parallel.Invoke(
        new ParallelOptions { CancellationToken = token },
        actions
    );
}
```


## 3.4. Dynamic Parallelism

### Problem
You have a more complex parallel situation where the structure and number of parallel tasks depends on information known only at runtime.

### Solution
In this case you should use `Task` class directly (without `Parallel` and `PLINQ` wrappers).

Here's an example of processing each node of a binary tree:
```csharp
void Traverse(Node current)
{
    DoExpensiveActionOnNode(current);

    if (current.Left != null)
    {
        Task.Factory.StartNew(
            () => Traverse(current.Left),
            CancellationToken.None,
            TaskCreationOptions.AttachedToParent,
            TaskScheduler.Default
        );
    }

    if (current.Right != null)
    {
        Task.Factory.StartNew(
            () => Traverse(current.Right),
            CancellationToken.None,
            TaskCreationOptions.AttachedToParent,
            TaskScheduler.Default
        );
    }
}

public void ProcessTree(Node root)
{
    var task = Task.Factory.StartNew(
        () => Traverse(root),
        CancellationToken.None,
        TaskCreateOptions.None,
        TaskScheduler.Default
    );
    task.Wait();
}
```

In this case parent tasks are processed before child tasks. How it works:
  * The root task is created in `ProcessTree()`.
  * At first it executes some heavy computation - `DoExpensiveActionOnNode()`.
  * Then it starts up to two child tasks.
  * There is no explicit waiting of parent tasks. But the are marked as `AttachedToParent`. Hence, parent task will wait until child tasks are completed
  * What I do not understand is how child tasks are attached to parent tasks. It looks like there is something stored on stack...

If you don't have a parent/child kind of situation, you can schedule any task to run after another by using a task `continuation`. The continuation is a separate task that executes when the original task completes:
```csharp
Task task = Task.Factory.StartNew(
    () => Thread.Sleep(TimeSpan.FromSeconds(2)),
    CancellationToken.None,
    TaskCreateOptions.None,
    TaskScheduler.Default
):

Task continuation = task.ContinueWith(
    t => Trace.WriteLine("Task is done"),
    CancellationToken.None,
    TaskConfiguratinoOptions.None,
    TaskScheduler.Default
);
```

### Discussion
The `Task` type serves two purposes in concurrent programming. It can be:
  * A parallel task. Parallel task may use blocking members, such as `Task.Wait`, `Task.Result`, `Task.WaitAll` and `Task.WaitAny`. Parallel tasks also commonly use `AttachedToParent` to create parent/child relationships between tasks. Parallel tasks should be created with `Task.Run` or `Task.Factory.StartNew`.
  * An asynchronous task. In contrast, asynchronous tasks should avoid blocking members and prefer `await`, `Task.WhenAll` and `Task.WhenAny`. Asynchronous tasks do not use `AttachedToParent`, but they can form an implicit kind of parent/child relationship by awaiting another task.


## 3.5. Parallel LINQ

### Problem
You have parallel processing to perform on a sequence of data, producing another sequence of data or a summary of that data.

### Solution
Simple example:
```csharp
static IEnumerable<int> MultiplyBy2(IEnumerable<int> values)
{
    return values.AsParallel().Select(item => item * 2);
}
```

The example may produce its outputs in any order. But you can also specify the order to be preserved:
```csharp
static IEnumerable<int> MultiplyBy2(IEnumerable<int> values)
{
    return values.AsParallel().AsOrdered().Select(item => item * 2);
}
```

Simple example of aggregation with PLINQ
```csharp
static int ParallelSum(IEnumerable<int> values)
{
    return values.AsParallel().Sum();
}
```

