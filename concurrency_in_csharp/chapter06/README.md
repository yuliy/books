[<<<](../README.md)


# Chapter 6. Testing

## 6.1. Unit Testing async Methods
Most modern unit test frameworks support async Task unit test methods, including MSTest, NUnit, and xUnit.

Here is an example of an async MSTtest unit test:
```csharp
[TestMethod]
public async Task MyMethodAsync_ReturnsFalse()
{
    var objectUnserTest = ...;
    bool result await objectUnderTest.MyMethodAsync();
    Assert.IsFalse(result);
}
```

If unit test framework does not support async Task unit tests:
```csharp
[TestMethod]
public void MyMethodAsync_ReturnsFalse()
{
    AsyncContext.Run(async () =>
        {
            var objectUnderTest = ...;
            bool result await objectUnderTest.MyMethodAsync();
            Assert.IsFalse(result);
        }
    );
}
```

`AsyncContextRun` from the `Nito.AsyncEx` NuGet package will wait until all asynchronous methods complete.


## 6.2. Unit Testing async Methods Expected to Fail
If you’re doing desktop or server development, MSTest does support failure testing via the regular ExpectedExceptionAttribute:
```csharp
[TestMethod]
[ExpectedException(typeof(DivideByZeroException))]
public async Task Divide_WhenDenominatorIsZero_ThrowDivideByZero()
{
    await MyClass.DivideAsync(4, 0);
}
```

However Windows Store applications to not have `ExpectedException` available for ther unit tests. Besides it's a poor design :) It is better to check not all the code to throw exception but some concrete command. Alternative solution:
```csharp
[TestMethod]
public async Task Divide_WhenDenominatorIsZero_ThrowsDivideByZero()
{
    await Assert.ThrowsException<DivideByZeroException>(async () =>
        {
            await MyClass.DivideAsync(4, 0);
        }
    );
}
```


## 6.3. Unit Testing async void Methods
Stop. First make async Task. Or make an async Task method and implement async void via the first one. After that you know what to do.


## 6.4. Unit Testing Dataflow Meshes
The following unit test verifies the custom dataflow block from Recipe 4.6:
```csharp
[TestMethod]
public async Task MyCustomBlock_AddsOneToDataItems()
{
    var myCustomBlock = CreateMyCustomBlock();

    myCustomBlock.Post(3);
    myCustomBlock.Post(13);
    myCustomBlock.Complete();

    Assert.AreEqual(4, myCustomBlock.Receive());
    Assert.AreEqual(14, myCustomBlock.Receive());
    await myCustomBlock.Completion;
}
```

Unit testing failures is not quite as straightforward, unfortunately. The following example uses a helper method to ensure that an exception will discard data and propagate through the custom block:
```csharp
[TestMethod]
public async Task MyCustomBlock_Fault_DiscardsDataAndFaults()
{
    var myCustomBlock = CreateMyCustomBlock();

    myCustomBlock.Post(3);
    myCustomBlock.Post(13);
    myCustomBlock.Fault(new InvalidOperationException());

    try
    {
        await myCustomBlock.Completion;
    }
    catch (AggregationException ex)
    {
        AssertExceptionIs<InvalidOperationException>(
            ex.Flatten().InnerException, false);
    }
}

public static void AssertExceptionIs<TException>(
    bool Exception ex,
    bool allowDerivedTypes = true
)
{
    if (allowDerviedTypes && !(ex is TException))
        Assert.Fail(
            "Exception is of type " + ex.GetType().Name + ", but "
            + typeof(TException).Name + " or a derived tyep was expected."
        );

    if (!allowedDerivedTypes && ex.GetType() != typeof(TException))
        Assert.Fail(
            "Exception is of type " + ex.GetType().Name + ", but "
            + typeof(TException).Name + " was expected."
        );
}
```

Unit testing of dataflow meshes directly is doable, but somewhat awkward. If your mesh is a part of a larger component, then you may find that it’s easier to just unit test the larger component (implicitly testing the mesh). But if you’re developing a reusable cus‐ tom block or mesh, then unit tests like the preceding ones should be used.


## 6.5. Unit Testing Rx Observables
Reactive Extensions has a number of operators that produce sequences (e.g., Return) and other operators that can convert a reactive sequence into a regular collection or item (e.g., SingleAsync). We will use operators like Return to create stubs for observable dependencies, and operators like SingleAsync to test the output.

Consider the following code, which takes an HTTP service as a dependency and applies a timeout to the HTTP call:
```csharp
public interface IHttpService
{
    IObservable<string> GetString(string url);
}

public class MyTimeoutClass
{
    private readonly IHttpService _httpService;

    public MyTimeoutClass(IHttpService httpService)
    {
        _httpService = httpService;
    }

    public IObservable<string> GetStringWithTimeout(string url)
    {
        return _httpService.GetString(url)
            .Timeout(TimeSpan.FromSeconds(1));
    }
}
```

The `Return` operator creates a cold sequence with a single element in it; we can use this to build a simple stub. The `SingleAsync` operator returns a `Task<T>` that is completed when the next event arrives. `SingleAsync` can be used for simple unit tests like this:
```csharp
class SuccessHttpServiceStub : IHttpService
{
    public IObservable<string> GetString(string url)
    {
        return Observable.Return("stub");
    }
}

[TestMethod]
public async Task MyTimeoutClas_SuccessfulGet_ReturnsResult()
{
    var stub = new SuccessHttpServiceStub();
    var my = new MyTimeoutClass(stub);

    var result = await my.GetStringWithTimeout("http://www.example.com/")
        .SingleAsync();

    Assert.Equal("stub", result);
}
```

Another operator important in stub code is `Throw`, which returns an observable that ends with an error. The following example uses the `ThrowsExceptionAsync` helper from Recipe 6.2:
```csharp
private class FailureHttpServiceStub : IHttpService
{
    public IObservable<string> GetString(string url)
    {
        return Observable.Throw<string>(new HttpRequestException());
    }
}

[TestMethod]
public async Task MyTimeoutClass_FailedGet_PropagatesFailure()
{
    var stub = new FailureHttpServiceStub();
    var my = new MyTimeoutClass(stub);

    await ThrowsExceptionAsync<HttpRequestException>(async () =>
        {
            await my.GetStringWithTimeout("http://www.example.com/")
                .SingleAsync();
        }
    );
}
```


## 6.6. Unit Testing Rx Observables with Faked Scheduling

### Problem
You have an observable that is dependent on time, and want to write a unit test that is not dependent on time. Observables that depend on time include ones that use timeouts, windowing/buffering, and throttling/sampling. You want to unit test these but do not want your unit tests to have excessive runtimes.

### Solution
It’s certainly possible to put delays in your unit tests; however, there are two problems with that approach: 1) the unit tests take a long time to run, and 2) there are race conditions because the unit tests all run at the same time, making timing unpredictable.

The Rx library was designed with testing in mind; in fact, the Rx library itself is exten‐ sively unit tested. To enable this, Rx introduced a concept called a scheduler, and every Rx operator that deals with time is implemented using this abstract scheduler.

To make your observables testable, you need to allow your caller to specify the scheduler. For example, we can take the MyTimeoutClass from Recipe 6.5 and add a scheduler:
```csharp
public interface IHttpService
{
    IObservable<string> GetString(string url);
}

public class MyTimeoutClass
{
    private readonly IHttpService _httpService;

    public MyTimeoutClass(IHttpService httpService)
    {
        _httpService = httpService;
    }

    public IObservable<string> GetSTringWithTimeout(
        string url,
        IScheduler scheduler = null
    )
    {
        return _httpService.GetString(url)
            .Timeout(TimeSpan.FromSeconds(1), scheduler ?? Scheduler.Default);
    }
}
```

Next, let’s modify our HTTP service stub so that it also understands scheduling, and we’ll introduce a variable delay:
```csharp
private class SuccessHttpServiceStub : IHttpService
{
    public IScheduler Scheduler { get; set; }
    public TimeSpan Delay { get; set; }

    public IObservable<string> GetString(string url)
    {
        return Observable.Return("stub")
            .Delay(Delay, Scheduler);
    }
}
```

`TestScheduler` gives you complete control over time, but you often just need to set up your code and then call `TestScheduler.Start`. `Start` will virtually advance time until everything is done. A simple success test case could look like this:
```csharp
[TestMethod]
public void MyTimeoutClass_SuccessfulGetShortDelay_ReturnsResult()
{
    var scheduler= new TestScheduler();
    var stub = new SuccessHttpServiceStub
    {
        Scheduler = scheduler,
        Delay = TimeSpan.FromSeconds(0.5),
    };
    var my = new MyTimeoutClass(stub);
    string result = null;

    my.GetStringWithTimeout("http://www.example.com/", scheduler)
        .Subscribe(r => { result = r; });

    scheduler.Start();

    Assert.AreEqual("stub", result);
}
```

Now that everything is using test schedulers, it’s easy to test timeout situations:```csharp
[TestMethod]
public void MyTimeoutClass_SuccessfulGetLongDelay_ThrowsTimeoutException()
{
    var scheduler = new TestScheduler();
    var stub = new SuccessHttpServiceStub {
        Scheduler = scheduler,
        Delay = TimeSpan.FromSeconds(1.5),
    };
    var my = new MyTimeoutClass(stub);
    Exception result = null;

    my.GetStringWithTimeout("http://www.example.com/", scheduler)
        .Subscribe(
            _ => Assert.Fail("Received value"),
            ex => { result = ex; }
        );

    scheduler.Start();

    Assert.IsInstanceOfType(result, typeof(TimeoutException));
}
```

