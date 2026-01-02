[<<<](../README.md)


# Chapter 7. Interop
Asynchronous, parallel, reactive —- each has its place, but how well do they work together?

In this chapter, we’ll look at various interop scenarios where we will learn how to combine these different approaches. We’ll learn that they complement each other, rather than compete; there is very little friction at the boundaries where one approach meets another.

## 7.1. Async Wrappers for "Async" Methods with "Completed" Events

### Problem
There is an older asynchronous pattern that uses methods named `OperationAsync` along with events named `OperationCompleted`. You wish to perform an operation like this and await the result.

> The `OperationAsync` and `OperationCompleted` pattern is called the Event-based Asynchronous Pattern (EAP). We’re going to wrap those into a `Task`-returning method that follows the `Task`-based Asynchronous Pattern (TAP).

### Solution
-

