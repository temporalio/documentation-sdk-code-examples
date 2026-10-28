using TemporalioDocs.Client.ConnectClient;

// Runs one sample by name, for example: dotnet run -- load-from-file
switch (args.FirstOrDefault())
{
    case "load-from-file":
        await LoadFromFile.RunAsync();
        break;
    case "load-from-env":
        await LoadFromEnv.RunAsync();
        break;
    case "load-profile":
        await LoadProfile.RunAsync();
        break;
    case "manual":
        await ManualConnect.RunAsync();
        break;
    default:
        Console.WriteLine("Usage: dotnet run -- <load-from-file|load-from-env|load-profile|manual>");
        break;
}
