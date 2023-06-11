# to run code type python number-sequence.py
word = input("Enter a number to tell you series: ")
iteration = int(input("Enter the number of times you want the sequence: "))
output_str = ""
i = 0
counter = 0
num = ""

for k in range(iteration):
    num = word
    while i < len(num):
        temp = num[i]
        while temp == num[counter + i]:
            counter += 1
            if (counter + i) >= len(num):
                break
        output_str += (str(counter) + temp)
        i = counter + i
        counter = 0

    word = output_str
    print("Output: " + output_str)
    counter = 0
    i = 0
    output_str = ""
