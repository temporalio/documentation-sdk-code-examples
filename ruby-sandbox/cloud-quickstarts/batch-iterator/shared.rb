# frozen_string_literal: true

# Shared constants for the Worker and client.
module BatchIterator
  # Keep the Worker and client pointed at the same value.
  TASK_QUEUE = ENV.fetch('TEMPORAL_TASK_QUEUE', '').then { |tq| tq.empty? ? 'quickstart-batch-iterator' : tq }

  # Records loaded and processed per Workflow run (one page).
  PAGE_SIZE = 5
end
