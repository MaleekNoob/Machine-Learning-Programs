
def fibonacci(n):
    if n == 1:
        return 0
    elif n == 2:
        return 1
    else:
        return (fibonacci(n-1) + fibonacci(n-2))

fibonacci_term = int(input("Enter a number to print series: "))
print("Fibonacci series:")
for i in range(1, fibonacci_term+1):
    print(fibonacci(i), end=" ")