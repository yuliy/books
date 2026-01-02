class Program
{
    static void Main(string[] args)
    {
        Console.WriteLine("This is ProcessingCmdLineArgsExample app.");
        for (int i = 0; i != args.Length; ++i)
        {
            Console.WriteLine("Arg #{0}: '{1}'", i, args[i]);
        }
    }
}
