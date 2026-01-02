#!/usr/bin/env python3

import time
import random

def main():
    print("\n<<< PYTHON >>>")
    size = 100 * 1000 * 1000
    print("Size: %s" % size);

    #
    start = time.time()
    array = generateRandomArray(size)
    printTimeCheckPoint(start, "Random array generated in ")

    #
    start = time.time()
    array.sort()
    printTimeCheckPoint(start, "Array sorted in ")

    #
    start = time.time()
    print("sorted: %s" % checkSorted(array))
    printTimeCheckPoint(start, "Sort correctness is checked in ")

def printTimeCheckPoint(start, msg):
    duration = (time.time() - start) * 1000.0
    print(msg + str(duration) + " ms")

def generateRandomArray(size):
    return [random.randint(-1e6, 1e6) for _ in range(size)]

def checkSorted(arr):
    for i in range(1, len(arr)):
        if arr[i] < arr[i-1]:
            return False
    return True

main()
