# frozen_string_literal: true

require 'minitest/autorun'
require 'securerandom'
require 'temporalio/testing'
require 'temporalio/worker'
require 'temporalio/worker/workflow_replayer'
require_relative '../activities'

# Each revision file defines MyWorkflow, so load each one into its own module, such as Revisions::PrePatch. The
# Workflow Type is the unqualified class name, so every revision is still the MyWorkflow type.
module Revisions
  def self.load(module_name, file)
    path = File.expand_path("../#{file}", __dir__)
    mod = const_set(module_name, Module.new)
    mod.module_eval(File.read(path), path)
    mod.const_get(:MyWorkflow)
  end

  PRE_PATCH = load(:PrePatch, 'pre_patch_workflow.rb')
  POST_PATCH = load(:PostPatch, 'post_patch_workflow.rb')
  PATCHED = load(:Patched, 'patched_workflow.rb')
  DEPRECATED = load(:Deprecated, 'deprecated_patch_workflow.rb')
end

class PatchingTest < Minitest::Test
  def self.env
    @env ||= Temporalio::Testing::WorkflowEnvironment.start_local(
      dev_server_existing_path: ENV.fetch('TEMPORAL_CLI_PATH', nil)
    ).tap { |env| Minitest.after_run { env.shutdown } }
  end

  # Runs a revision to completion and returns its result and Event History.
  def run_revision(workflow)
    client = self.class.env.client
    task_queue = "versioning-#{SecureRandom.uuid}"
    Temporalio::Worker.new(
      client:, task_queue:, workflows: [workflow], activities: [PrePatchActivity, PostPatchActivity]
    ).run do
      handle = client.start_workflow(workflow, id: "wf-#{SecureRandom.uuid}", task_queue:)
      [handle.result, handle.fetch_history]
    end
  end

  def replay(workflow, history)
    Temporalio::Worker::WorkflowReplayer.new(workflows: [workflow]).replay_workflow(history)
  end

  def test_every_revision_uses_the_same_workflow_type
    names = [Revisions::PRE_PATCH, Revisions::POST_PATCH, Revisions::PATCHED, Revisions::DEPRECATED].map do |w|
      Temporalio::Workflow::Definition::Info.from_class(w).name
    end
    assert_equal ['MyWorkflow'], names.uniq
  end

  def test_post_patch_code_breaks_pre_patch_history
    result, history = run_revision(Revisions::PRE_PATCH)
    assert_equal 'pre-patch', result
    assert_raises(Temporalio::Workflow::NondeterminismError) { replay(Revisions::POST_PATCH, history) }
  end

  def test_patched_code_replays_pre_patch_history_and_runs_new_branch
    _, pre_history = run_revision(Revisions::PRE_PATCH)
    replay(Revisions::PATCHED, pre_history)

    result, patched_history = run_revision(Revisions::PATCHED)
    assert_equal 'post-patch', result
    assert(patched_history.events.any?(&:marker_recorded_event_attributes))
  end

  def test_deprecated_patch_replays_patched_history
    _, patched_history = run_revision(Revisions::PATCHED)
    replay(Revisions::DEPRECATED, patched_history)

    result, = run_revision(Revisions::DEPRECATED)
    assert_equal 'post-patch', result
  end

  def test_patched_history_breaks_code_without_the_patch
    _, patched_history = run_revision(Revisions::PATCHED)
    assert_raises(Temporalio::Workflow::NondeterminismError) { replay(Revisions::POST_PATCH, patched_history) }
  end

  def test_removing_the_patch_replays_deprecated_history
    _, deprecated_history = run_revision(Revisions::DEPRECATED)
    replay(Revisions::POST_PATCH, deprecated_history)
  end
end
