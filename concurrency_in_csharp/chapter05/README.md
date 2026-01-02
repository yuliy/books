[<<<](../README.md)


# Chapter 5. Rx Basics
Rx = Reactive Extensions.

Reminder:
  * LINQ - pull model
  * Rx - push model

You can think of Rx as LINQ to events (based on `IObservable<T>`). Whereas LINQ to objects is base on `IEnumeration<T>` and LINQ to entities is based on `IQuerable<T>`. There are also other kinds of providers for LINQ.

So, Rx works with sequences of events. Events are processed as they arrive (push model). Query defines how the program reacts as events arrive. Rx is built on top of LINQ, adding some extension methods.


## 5.1. Converting .NET Events

### Problem
You have an event that you need to treat as an Rx input stream, producing some data via OnNext each time the event is raised.

### Solution
```csharp
var progress = new Progress<int>();
var progressReports = Observable.FromEventPattern<int>(
    handler => progress.ProgressChanged += handler,
    handler => progress.ProgressChanged -= handler
);
progressReports.Substribe(data => Trace.WriteLine("OnNext: " + data.EventArgs));
```


## 5.2. Sending Notifications to a Context
-

## 5.3. Grouping Event Data with Windows and Buffers
Rx provides a pair of operators that group incoming sequences: `Buffer` and `Window`. `Buffer` will hold on to the incoming events until the group is complete. `Window` will logically group the incoming events but will pass them along as they arrive.

Return type of `Buffer` is `IObservable<IList<T>>` (an event stream of collections). Return type of `Window` is `IObservable<IObservable<T>>` (an event stream of streams).

This example uses the Interval operator to create OnNext notifications once a second and then buffers them two at a time:
```csharp
private void Button_Click(object sender, RoutedEventArgs e)
{
    Observable.Interval(TimeSpan.FromSeconds(1))
        .Buffer(2)
        .Subscribe(x => Trace.WriteLine(
            DateTime.Now.Second + ": Got " + x[0] + " and " + x[1]
        )
    );
}
```

The following is a similar example using Window to create groups of two events:
```csharp
private void Button_Click(object sender, RoutedEventArgs e)
{
    Observable.Interval(TimeSpan.FromSeconds(1))
        .Window(2)
        .Subscribe(group =>
            {
                Trace.WriteLine(DateTime.Now.Second + ": Starting new group");
                group.Subscribe(
                    x => Trace.WriteLine(DateTime.Now.Second + ": Saw " + x),
                    () => Trace.WriteLine(DateTime.Now.Second + " Ending group"
                );
            }
        )
    );
}
```

These examples illustrate the difference between Buffer and Window. Buffer waits for all the events in its group and then publishes a single collection. Window groups events the same way, but publishes the events as they come in.


## 5.4. Taming Event Streams with Throttling and Sampling
A common problem with writing reactive code is when the events come in too quickly. A fast-moving stream of events can overwhelm your program’s processing.

Rx provides operators specifically for dealing with a flood of event data. The Throt tle and Sample operators give us two different ways to tame fast input events.

The Throttle operator establishes a sliding timeout window. When an incoming event arrives, it resets the timeout window. When the timeout window expires, it publishes the last event value that arrived within the window.

This example monitors mouse movements but uses `Throttle` to only report updates once the mouse has stayed still for a full second:
```csharp
private void Button_Click(object sender, RoutedEventArgs e)
{
    Observable.FromEventPatter<MouseEventHandler, MouseEventArgs>(
            handler => (s, a) => handler(s, a),
            handler => MouseMove += handler,
            handler => MouseMove -= handler
        )
        .Select(x => x.EventArgs.GetPosition(this))
        .Throttle(TimeSpan.FromSeconds(1))
        .Subscribe(x => Trace.WriteLine(
            DateTime.Now.Second + ": Saw " + (x.X + x.Y)
        )
    );
}
```

`Sample` takes a different approach to taming fast-moving sequences. Sample establishes a regular timeout period and publishes the most recent value within that window each time the timeout expires. If there were no values received within the sample period, then no results are published for that period.


## 5.5. Timeouts
-

