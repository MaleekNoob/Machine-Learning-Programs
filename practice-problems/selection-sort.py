
def selection_sort(list):

    temp = 0

    for i in range (0, len(list)):
        for j in range(i, len(list)):
            if list[i] > list[j]:
                temp = list[i]
                list[i] = list[j]
                list[j] = temp

    return list


list = [1, 5, 2, 4, 3, 0, -8, 9, 7, 6, -2, 4, 6, -13, 45, -9, 44]
print(selection_sort(list))

