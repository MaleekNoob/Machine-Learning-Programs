#!/bin/python3

import math
import os
import random
import re
import sys

#
# Complete the 'timeConversion' function below.
#
# The function is expected to return a STRING.
# The function accepts STRING s as parameter.
#

def timeConversion(s):
    sub_str = s[-2:]
    hours = s[:2]
    remaining = s[2:-2]
    int_hours = int(hours)

    if (sub_str == 'PM'):
        int_hours = (12 + int_hours % 12)
    elif (sub_str == 'AM'):
        int_hours %= 12

    hours = str(int_hours)

    if int_hours % 10 == int_hours:
        hours = '0' + hours
        
    hours += remaining

    return hours


s = '01:01:00AM'

print(timeConversion(s))