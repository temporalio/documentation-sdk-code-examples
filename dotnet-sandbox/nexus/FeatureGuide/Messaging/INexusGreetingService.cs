namespace TemporalioDocs.Nexus.FeatureGuide.Messaging;

using NexusRpc;

[NexusService]
public interface INexusGreetingService
{
    [NexusOperation]
    ApproveOutput Approve(ApproveInput input);

    public record ApproveInput(string UserId, string Name);

    public record ApproveOutput;
}
