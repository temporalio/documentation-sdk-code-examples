# frozen_string_literal: true

# Starts MyWorkflow from upsert_workflow.rb with a custom Search Attribute.
# Create the attribute first:
#   temporal operator search-attribute create --name my-keyword --type Keyword
require 'temporalio/client'
require 'temporalio/search_attributes'
require 'temporalio/workflow'

my_client = Temporalio::Client.connect(ENV.fetch('TEMPORAL_ADDRESS', 'localhost:7233'), 'default')

# Names the Workflow Type to start. A Worker running upsert_workflow.rb executes it.
class MyWorkflow < Temporalio::Workflow::Definition; end

# @@@SNIPSTART ruby-observability-start-with-search-attributes
# Predefined search attribute key, usually a global somewhere
MY_KEYWORD_KEY = Temporalio::SearchAttributes::Key.new(
  'my-keyword',
  Temporalio::SearchAttributes::IndexedValueType::KEYWORD
)

# ...

# Start workflow with the search attribute set
handle = my_client.start_workflow(
  MyWorkflow, 'some-input',
  id: 'my-workflow-id', task_queue: 'my-task-queue',
  search_attributes: Temporalio::SearchAttributes.new({ MY_KEYWORD_KEY => 'some-value' })
)
# @@@SNIPEND

puts "Started #{handle.id}"
