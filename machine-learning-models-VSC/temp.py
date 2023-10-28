import numpy as np

def moving_average_forecast(series, window_size):
  mov = np.cumsum(series)
  
  return mov

arr = np.array([1, 2, 3, 4, 5, 6, 7, 8, 9])
print(moving_average_forecast(arr, 2))