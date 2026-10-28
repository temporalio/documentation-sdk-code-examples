using TemporalioDocs.Activities.StandaloneActivities;

switch (args.ElementAtOrDefault(0))
{
    case "worker":
        await StandaloneWorker.RunAsync();
        break;
    case "execute-activity":
        await StandaloneClient.RunAsync();
        break;
    default:
        throw new ArgumentException("Must pass 'worker' or 'execute-activity' as the single argument");
}
