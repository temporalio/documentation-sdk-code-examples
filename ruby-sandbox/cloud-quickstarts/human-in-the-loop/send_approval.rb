# frozen_string_literal: true

require 'temporalio/client'
require 'time'
require_relative 'connection'
require_relative 'human_in_the_loop_workflow'
require_relative 'shared'

USAGE = 'Usage: bundle exec ruby send_approval.rb <approve|reject> [notes]'

decision = ARGV[0]&.downcase
unless %w[approve reject].include?(decision)
  puts USAGE
  exit 1
end
notes = ARGV[1]

# Same environment-configuration lookup the Worker and client use.
args, kwargs = HumanInTheLoop.connect_options
client = Temporalio::Client.connect(*args, **kwargs)

# Deliver the reviewer's decision to the waiting Workflow as a Signal. With no
# request_id, it applies to whichever request is pending.
handle = client.workflow_handle(HumanInTheLoop::WORKFLOW_ID)
handle.signal(
  HumanInTheLoop::HumanInTheLoopWorkflow.approval_decision,
  { 'approved' => decision == 'approve', 'reviewer_notes' => notes, 'decided_at' => Time.now.utc.iso8601 }
)

puts "Sent #{decision} decision to Workflow #{HumanInTheLoop::WORKFLOW_ID}"
