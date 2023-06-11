
def bubble_sort(array):

    for i in range(0, len(array)):
        for j in range(0, (len(array) - i) - 1):
            if array[j] > array[j + 1]:
                temp = array[j]
                array[j] = array[j + 1]
                array[j + 1] = temp

    return array


array = [1, 5, 2, 4, 3, 0, -8, 9, 7, 6, -2, 4, 6, -13, 45, -9, 44]
print(bubble_sort(array))