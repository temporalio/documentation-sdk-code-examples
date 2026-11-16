# frozen_string_literal: true

# Shared constants for the Worker and clients.
#
# The values passed between the Workflow, Activities, and clients are Hashes with
# snake_case String keys, which the default JSON converter round-trips as-is. Signal
# input from the Temporal CLI or UI, for example {"approved": true}, arrives in the
# same shape, matching the other language versions of this sample:
#
#   WorkflowInput:    { 'user_request', 'approval_timeout_seconds' }
#   ProposedAction:   { 'action_type', 'description', 'reasoning', 'risky_action' }
#   ApprovalRequest:  { 'request_id', 'proposed_action', 'context', 'requested_at' }
#   ApprovalDecision: { 'approved', 'request_id', 'reviewer_notes', 'decided_at' }
#                     Leave out 'request_id' to apply the decision to whichever
#                     request is pending.
module HumanInTheLoop
  def self.env_or(key, fallback)
    value = ENV.fetch(key, '')
    value.empty? ? fallback : value
  end

  # Keep the Worker and clients pointed at the same value.
  TASK_QUEUE = env_or('TEMPORAL_TASK_QUEUE', 'quickstart-human-in-the-loop')

  # A fixed Workflow ID, so approval commands can be written as fixed text. Only one
  # Workflow with this ID can run at a time.
  WORKFLOW_ID = env_or('HUMAN_IN_THE_LOOP_WORKFLOW_ID', 'human-in-the-loop')
end
