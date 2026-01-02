class Program
{
    static void Main(string[] args)
    {
        ShowEnvironmentDetails();
    }

    static void ShowEnvironmentDetails()
    {
        foreach (string drive in System.Environment.GetLogicalDrives())
        {
            Console.WriteLine("Drive: {0}", drive);
        }

        Console.WriteLine("OS: {0}", System.Environment.OSVersion);
        Console.WriteLine("Number of processors: {0}", System.Environment.ProcessorCount);
        Console.WriteLine(".NET Core Version: {0}", System.Environment.Version);
    }
}
