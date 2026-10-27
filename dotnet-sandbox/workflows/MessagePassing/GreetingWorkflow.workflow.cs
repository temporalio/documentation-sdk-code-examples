namespace TemporalioDocs.Workflows.MessagePassing;

using Temporalio.Exceptions;
using Temporalio.Workflows;

[Workflow]
public class GreetingWorkflow
{
    public enum Language
    {
        Arabic,
        Chinese,
        English,
        French,
        Hindi,
        Portuguese,
        Spanish,
    }

    private readonly Dictionary<Language, string> greetings = new()
    {
        [Language.Chinese] = "你好，世界",
        [Language.English] = "Hello, world",
    };

    private bool approvedForRelease;
    private string? approverName;
    private string? submittedGreeting;

    [WorkflowRun]
    public async Task<string> RunAsync()
    {
        await Workflow.WaitConditionAsync(() => approvedForRelease);
        await Workflow.WaitConditionAsync(() => Workflow.AllHandlersFinished);
        return $"{submittedGreeting ?? greetings[CurrentLanguage]} (approved by {approverName})";
    }

    // @@@SNIPSTART dotnet-message-passing-query-method
    public record GetLanguagesInput(bool IncludeUnsupported);

    [WorkflowQuery]
    public IList<Language> GetLanguages(GetLanguagesInput input) =>
        Enum.GetValues<Language>().
            Where(language => input.IncludeUnsupported || greetings.ContainsKey(language)).
            ToList();
    // @@@SNIPEND

    // @@@SNIPSTART dotnet-message-passing-query-property
    [WorkflowQuery]
    public Language CurrentLanguage { get; private set; } = Language.English;
    // @@@SNIPEND

    // @@@SNIPSTART dotnet-message-passing-signal
    public record ApproveInput(string Name);

    [WorkflowSignal]
    public async Task ApproveAsync(ApproveInput input)
    {
        approvedForRelease = true;
        approverName = input.Name;
    }
    // @@@SNIPEND

    [WorkflowSignal]
    public async Task SubmitGreetingAsync(string greeting) => submittedGreeting = greeting;

    // @@@SNIPSTART dotnet-message-passing-update
    [WorkflowUpdateValidator(nameof(SetCurrentLanguageAsync))]
    public void ValidateLanguage(Language language)
    {
        if (!greetings.ContainsKey(language))
        {
            throw new ApplicationFailureException($"{language} is not supported");
        }
    }

    [WorkflowUpdate]
    public async Task<Language> SetCurrentLanguageAsync(Language language)
    {
        var previousLanguage = CurrentLanguage;
        CurrentLanguage = language;
        return previousLanguage;
    }
    // @@@SNIPEND

    // @@@SNIPSTART dotnet-message-passing-async-update
    private readonly Mutex mutex = new();

    [WorkflowUpdate]
    public async Task<Language> SetLanguageAsync(Language language)
    {
        // 👉 Use a mutex here to ensure that multiple calls to SetLanguageAsync are processed in order.
        await mutex.WaitOneAsync();
        try
        {
            if (!greetings.ContainsKey(language))
            {
                var greeting = await Workflow.ExecuteActivityAsync(
                    (MyActivities acts) => acts.CallGreetingServiceAsync(language),
                    new() { StartToCloseTimeout = TimeSpan.FromSeconds(10) });
                if (greeting == null)
                {
                    // 👉 An update validator cannot be async, so cannot be used to check that the remote
                    // CallGreetingServiceAsync supports the requested language. Throwing ApplicationFailureException
                    // will fail the Update, but the WorkflowExecutionUpdateAccepted event will still be
                    // added to history.
                    throw new ApplicationFailureException(
                        $"Greeting service does not support {language}");
                }
                greetings[language] = greeting;
            }
            var previousLanguage = CurrentLanguage;
            CurrentLanguage = language;
            return previousLanguage;
        }
        finally
        {
            mutex.ReleaseMutex();
        }
    }
    // @@@SNIPEND
}
