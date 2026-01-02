[<<<](../README.md)


# Chapter 4. Dataflow Basics
TPL Dataflow is a powerful library that allows you to create a mesh or pipeline and then (asynchronously) send your data through it. Dataflow is a very declarative style of coding; normally, you completely define the mesh first and then start processing data. Each mesh is comprised of various blocks that are linked to each other.

## 4.1. Linking Blocks
```csharp
var multiplyBlock = new TransformBlock<int, int>(item => item * 2);
var subtractBlock = new TransformBlock<int, int>(item => item - 2);

// After linking, values that exit multiplyBlock will enter subtractBlock.
multiplyBlock.LinkTo(subtractBlock);
```

By default, linked dataflow blocks only propagate data; they do not propagate completion (or errors). You can do it as follows:
```csharp
var multiplyBlock = new TransformBlock<int, int>(item => item * 2);
var subtractBlock = new TransformBlock<int, int>(item => item - 2);

var options = new DataflowLinkOptions { PropagateCompletion = true };
multiplyBlock.LinkTo(subtractBlock, options);

// The first block's completion is automatically propagated to the second block.
multiplyBlock.Complete();
await subtractBlock.Completion;
```


## 4.2. Propagating Errors
If a delegate passed to a dataflow block throws an exception, then that block will enter a faulted state. When a block is in a faulted state, it will drop all of its data (and stop accepting new data). The block in this code will never produce any output data; the first value raises an exception, and the second value is just dropped:
```csharp
var block = new TransformBlock<int, int>(item =>
{
    if (item == 1)
        throw new InvalidOperationException("Blech.");
    return item * 2; }
);

block.Post(1);
block.Post(2);
```

To catch exceptions from a dataflow block, await its Completion property.
```csharp
try
{
    var block = new TransformBlock<int, int>(
        item =>
        {
            if (item == 1)
                throw new InvalidOperationException("Blech.");
            return item * 2;
        }
    );

    block.Post(1);
    await block.Completion;
}
catch (InvalidOperationException)
{
    // ...
}
```

When you propagate completion using the PropagateCompletion link option, errors are also propagated. However, the exception is passed to the next block wrapped in an AggregateException. The AggregateException.Flatten method simplifies error handling in this scenario.


## 4.3. Unlinking Blocks

### Problem
During processing, you need to dynamically change the structure of your dataflow. This is an advanced scenario and is hardly ever needed.

### Solution
You can link or unlink dataflow blocks at any time; data can be freely passing through the mesh and it is still safe to link or unlink at any time. Both linking and unlinking are fully threadsafe.

When you create a dataflow block link, keep the `IDisposable` returned by the `LinkTo` method, and dispose of it when you want to unlink the blocks:
```csharp
var multiplyBlock = new TransformBlock<int, int>(item => item * 2);
var subtractBlock = new TransformBlock<int, int>(item => item - 2);

IDisposable link = multiplyBlock.LinkTo(subtractBlock);
multiplyBlock.Post(1);
multiplyBlock.Post(2);

// Unlink the blocks.
// The data posted above may or may not have already gone through the link.
// In real-world code, consider a using block rather than calling Dispose.
link.Dispose();
```


## 4.4. Throttling Blocks

### Problem
You have a fork scenario in your dataflow mesh and want the data to flow in a load- balancing way.

### Solution
```csharp
var sourceBlock = new BufferBlock<int>();
var options = new DataflowBlockOptions { BoundedCapacity = 1 };
var targetBlockA = new BufferBlock<int>(options);
var targetBlockB = new BufferBlock<int>(options);

sourceBlock.LinkTo(targetBlockA);
sourceBlock.LinkTo(targetBlockB);
```


## 4.5. Parallel Processing with Dataflow Blocks
Every dataflow mesh has natural parallelism built in.

```csharp
var multiplyBlock = new TransformBlock<int, int>(
    item => item * 2,
    new ExecutionDataflowBlockOptions
        {
            MaxDegreeOfParallelism = DataflowBlockOptions.Unbounded
        }
);

var subtractBlock = new TransformBlock<int, int>(item => item - 2);
multiplyBlock.LinkTo(subtractBlock);
```

The MaxDegreeOfParallelism option makes parallel processing within a block easy to do. What is not so easy is determining which blocks need it. One technique is to pause dataflow execution in the debugger, where you can see the number of data items queued up (that have not yet been processed by the block). This can be an indication that some restructuring or parallelization would be helpful.


## 4.5. Creating Custom Blocks
The following code creates a custom dataflow block out of two blocks, propa‐ gating data and completion:
```csharp
IPropagatorBlock<int, int> CreateMyCustomBlock()
{
    var multiplyBlock = new TransformBlock<int, int>(item => item * 2);
    var addBlock = new TransformBlock<int, int>(item => item + 2);
    var divideBlock = new TransformBlock<int, int>(item => item / 2);

    var flowCompletion = new DataflowLinkOptions { PropagateCompletion = true };
    multiplyBlock.LinkTo(addBlock, flowCompletion);
    addBlock.LinkTo(divideBlock, flowCompletion);

    return DataflowBlock.Encapsulate(multiplyBlock, divideBlock);
}
```

