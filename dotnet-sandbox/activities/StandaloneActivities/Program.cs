using TemporalioDocs.Activities.StandaloneActivities;

switch (args.ElementAtOrDefault(0))
{
    case "worker":
        await StandaloneWorker.RunAsync();
        break;
    case "execute-activity":
        await StandaloneClient.RunAsync();
        break;
    case "start-activity":
        await StartActivityClient.RunAsync();
        break;
    case "list-activities":
        await ListActivitiesClient.RunAsync();
        break;
    case "count-activities":
        await CountActivitiesClient.RunAsync();
        break;
    default:
        throw new ArgumentException(
            "Must pass 'worker', 'execute-activity', 'start-activity', 'list-activities', or 'count-activities' as the single argument");
}
