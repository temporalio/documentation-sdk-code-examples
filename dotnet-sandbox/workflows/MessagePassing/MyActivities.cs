namespace TemporalioDocs.Workflows.MessagePassing;

using Temporalio.Activities;
using Language = GreetingWorkflow.Language;

public record FetchedData(int X, int Y);

// @@@SNIPSTART dotnet-message-passing-activity
public class MyActivities
{
    private static readonly Dictionary<Language, string> Greetings = new()
    {
        [Language.Arabic] = "مرحبا بالعالم",
        [Language.Chinese] = "你好，世界",
        [Language.English] = "Hello, world",
        [Language.French] = "Bonjour, monde",
        [Language.Hindi] = "नमस्ते दुनिया",
        [Language.Spanish] = "Hola mundo",
    };

    [Activity]
    public async Task<string?> CallGreetingServiceAsync(Language language)
    {
        // Pretend that we are calling a remove service
        await Task.Delay(200);
        return Greetings.TryGetValue(language, out var value) ? value : null;
    }
    // @@@SNIPEND

    [Activity]
    public async Task<FetchedData> FetchDataAsync()
    {
        await Task.Delay(200);
        return new(1, 2);
    }
}
