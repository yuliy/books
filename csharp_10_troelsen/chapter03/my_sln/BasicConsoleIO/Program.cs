class Program
{
    static void Main()
    {
        Console.WriteLine("***** Basic Console I/O *****");
        GetUserData();
        FormatNumericalData();
        DataTypeFunctionality();
        CharTypeFunctionality();
    }

    static void GetUserData()
    {
        // Get name and age.
        Console.Write("Please enter your name: ");
        string? userName = Console.ReadLine();
        Console.Write("Please enter your age: ");
        string? userAge = Console.ReadLine();

        // Change echo color, just for fun.
        ConsoleColor prevColor = Console.ForegroundColor;
        Console.ForegroundColor = ConsoleColor.Yellow;

        // Echo to the console.
        Console.WriteLine(
            "Hello {0}! You are {1} years old.",
            userName, userAge
        );

        // Restore previous color.
        Console.ForegroundColor = prevColor;
        Console.WriteLine();
    }

    static void FormatNumericalData()
    {
        Console.WriteLine("The value 99999 in various formats:");
        Console.WriteLine("c format: {0:c}", 99999);
        Console.WriteLine("d9 format: {0:d9}", 99999);
        Console.WriteLine("f3 format: {0:f3}", 99999);
        Console.WriteLine("n format: {0:n}", 99999);

        // Notice that upper- or lowercasing for hex
        // determines if letters are upper- or lowercase.
        Console.WriteLine("E format: {0:E}", 99999);
        Console.WriteLine("e format: {0:e}", 99999);
        Console.WriteLine("X format: {0:X}", 99999);
        Console.WriteLine("x format: {0:x}", 99999);
        Console.WriteLine();
    }

    static void DataTypeFunctionality()
    {
        Console.WriteLine("=> Data type Functionality:");

        Console.WriteLine("Max of int: {0}", int.MaxValue);
        Console.WriteLine("Min of int: {0}", int.MinValue);
        Console.WriteLine("Max of double: {0}", double.MaxValue);
        Console.WriteLine("Min of double: {0}", double.MinValue);
        Console.WriteLine("double.Epsilon: {0}", double.Epsilon);
        Console.WriteLine("double.PositiveInfinity: {0}", double.PositiveInfinity);
        Console.WriteLine("double.NegativeInfinity: {0}", double.NegativeInfinity);
        Console.WriteLine("bool.FalseString: {0}", bool.FalseString);
        Console.WriteLine("bool.TrueString: {0}", bool.TrueString);
        Console.WriteLine();
    }

    static void CharTypeFunctionality()
    {
        Console.WriteLine("=> Chart type functionality:");

        char myChar = 'a';
        Console.WriteLine("char.IsDigit('a'): {0}", char.IsDigit(myChar));
        Console.WriteLine("char.IsLetter('a'): {0}", char.IsLetter(myChar));
        Console.WriteLine("char.IsWhiteSpace('Hello There', 5): {0}",
            char.IsWhiteSpace("Hello There", 5));
        Console.WriteLine("char.IsWhiteSpace('Hello There', 6): {0}",
            char.IsWhiteSpace("Hello There", 6));
        Console.WriteLine("char.IsPunctuation('?'): {0}", char.IsPunctuation('?'));
        Console.WriteLine();
    }
}