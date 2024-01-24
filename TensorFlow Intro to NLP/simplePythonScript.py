term = int(input("Enter a number: "))
if term < 0:
    print("Sorry, factorial does not exist for negative numbers")

elif term == 0:
    print("The factorial of 0 is 1")

else:
    factorial = 1
    for i in range(1, term + 1):
        factorial = factorial * i
    print("The factorial of", term, "is", factorial)