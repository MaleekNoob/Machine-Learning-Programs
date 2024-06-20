#!/bin/python3

import math
import os
import random
import re
import sys

#
# Complete the 'countingSort' function below.
#
# The function is expected to return an INTEGER_ARRAY.
# The function accepts INTEGER_ARRAY arr as parameter.
#

def countingSort(arr):
    newArr = [0] * 100

    for value in arr:
        if (value > 99):
            value = 0
        
        newArr[value] += 1

    returnArr = []

    for i in range(len(newArr)):
        val = newArr[i]
        for j in range(val):
            returnArr.append(i)

    return returnArr


arr = [48, 3, 19, 22, 1, 76, 34, 89, 12, 64, 95, 5, 17, 82, 2, 38, 6, 100, 40, 90, 7, 29, 51, 8, 61, 31, 20, 43, 92, 53]
    
print(countingSort(arr))

