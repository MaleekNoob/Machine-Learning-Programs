
def fibonacci(n):
    if n == 1:
        return 0
    elif n == 2:
        return 1
    else:
        return (fibonacci(n-1) + fibonacci(n-2))
    
def arithimatic(n):
    if n == 1:
        return 0
    else:
        return (arithimatic(n-1) + 2)
    
def geometric(n):
    if n == 1:
        return 1
    else:
        return (geometric(n-1) * 2)


choice = input("1. Print Fibonacci series\n2. Print Arithimatic series\n3. Print Geometric series\nEnter your choice: ")
if choice == "1":
    fibonacci_term = int(input("Enter a number to print series: "))
    print("Fibonacci series:")
    for i in range(1, fibonacci_term+1):
        print(fibonacci(i), end=" ")
    print()

elif choice == "2":
    arithimatic_term = int(input("Enter a number to print series: "))
    print("Arithimatic series:")
    for i in range(1, arithimatic_term+1):
        print(arithimatic(i), end=" ")
    print()

elif choice == "3":
    geometric_term = int(input("Enter a number to print series: "))
    print("Geometric series:")
    for i in range(1, geometric_term+1):
        print(geometric(i), end=" ")
    print()

else:
    print("Invalid choice")