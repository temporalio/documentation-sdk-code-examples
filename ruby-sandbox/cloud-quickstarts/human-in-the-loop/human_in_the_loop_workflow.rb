# frozen_string_literal: true

require 'temporalio/workflow'
require 'time'
require_relative 'activities'

module HumanInTheLoop
  # An AI agent that asks a human to approve risky actions.
  #
  # An LLM proposes an action for the user's request. Safe actions run right away.
  # Risky actions wait for an approval decision delivered by a Signal, with a
  # durable timeout in case no one responds.
  class HumanInTheLoopWorkflow < Temporalio::Workflow::Definition
    # Tells the LLM how to analyze a request and assess its risk.
    SYSTEM_INSTRUCTIONS = <<~TEXT
      You are an AI assistant that analyzes user requests and proposes actions.
      For each request, you should:
      1. Determine what action needs to be taken
      2. Provide a clear description of the action
      3. Explain your reasoning for why this action addresses the request
      4. Assess whether this is a risky action that requires human approval

      Consider an action risky if it:
      - Could cause data loss or corruption
      - Could affect production systems or critical infrastructure
      - Could have financial or legal implications
      - Could impact user experience or system availability
      - Involves deleting, modifying, or overwriting important data
      - Could expose sensitive information or security vulnerabilities

      Be thorough and clear in your analysis.

      Respond with a JSON object in this structure:

      {
        "action_type": "A short name for the action (e.g., \\"delete_test_data\\")",
        "description": "A clear description of what the action will do",
        "reasoning": "Your explanation for why this action addresses the request",
        "risky_action": true or false indicating whether this action is considered risky
      }
    TEXT

    def initialize
      # The Signal handler writes these; the wait condition reads them.
      @current_decision = nil
      @pending_request_id = nil
    end

    # input is a WorkflowInput Hash. Returns a sentence describing whether the
    # action ran, was rejected, or timed out.
    def execute(input)
      user_request = input['user_request']
      timeout_seconds = input.fetch('approval_timeout_seconds', 300)

      proposed = propose_action(user_request)
      Temporalio::Workflow.logger.info(
        "AI proposed action: #{proposed['action_type']} (risky: #{proposed['risky_action']})"
      )

      return "Action completed successfully (auto-approved): #{execute_action(proposed)}" unless proposed['risky_action']

      decision = request_approval(proposed, user_request, timeout_seconds)
      return "Action cancelled: approval request timed out after #{timeout_seconds} seconds" if decision.nil?
      return "Action rejected. Reviewer notes: #{decision['reviewer_notes'] || 'None provided'}" unless decision['approved']

      "Action completed successfully: #{execute_action(proposed)}"
    end

    # Delivers a reviewer's approve or reject decision (an ApprovalDecision Hash).
    workflow_signal name: 'approval_decision'
    def approval_decision(decision)
      request_id = decision['request_id']
      if @pending_request_id.nil?
        Temporalio::Workflow.logger.warn('Ignoring decision: no approval request is pending')
      # A decision without a request_id applies to the pending request. One with a
      # request_id must match it, so a stale decision is ignored.
      elsif request_id.nil? || request_id == @pending_request_id
        @current_decision = decision
      else
        Temporalio::Workflow.logger.warn(
          "Ignoring decision for request #{request_id}; expected #{@pending_request_id}"
        )
      end
    end

    private

    def propose_action(user_request)
      Temporalio::Workflow.execute_activity(
        ProposeActionActivity,
        { 'model' => 'gpt-4o-mini', 'instructions' => SYSTEM_INSTRUCTIONS, 'input' => user_request },
        start_to_close_timeout: 30
      )
    end

    def execute_action(action)
      Temporalio::Workflow.execute_activity(ExecuteActionActivity, action, start_to_close_timeout: 60)
    end

    # Notifies a reviewer and waits for a decision. Returns nil if the timeout
    # fires first.
    def request_approval(action, user_request, timeout_seconds)
      @current_decision = nil
      # Workflow.random is deterministic, so this UUID is safe on replay.
      @pending_request_id = Temporalio::Workflow.random.uuid

      Temporalio::Workflow.execute_activity(
        NotifyApprovalNeededActivity,
        {
          'request_id' => @pending_request_id,
          'proposed_action' => action,
          'context' => user_request,
          'requested_at' => Temporalio::Workflow.now.utc.iso8601
        },
        start_to_close_timeout: 10
      )

      # A durable timer: the Workflow uses no compute while it waits, and the
      # timeout survives Worker restarts.
      Temporalio::Workflow.timeout(timeout_seconds) do
        Temporalio::Workflow.wait_condition { @current_decision }
      end
    rescue Timeout::Error
      nil
    end
  end
end
