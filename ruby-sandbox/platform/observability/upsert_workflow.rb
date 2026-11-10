# frozen_string_literal: true

require 'temporalio/search_attributes'
require 'temporalio/workflow'

# @@@SNIPSTART ruby-observability-upsert-search-attributes
# Predefined search attribute key, usually a global somewhere
MY_KEYWORD_KEY = Temporalio::SearchAttributes::Key.new(
  'my-keyword',
  Temporalio::SearchAttributes::IndexedValueType::KEYWORD
)

# ...

class MyWorkflow < Temporalio::Workflow::Definition
  def execute
    # ...

    Temporalio::Workflow.upsert_search_attributes(MY_KEYWORD_KEY.value_set('some-new-value'))

    # ...
  end
end
# @@@SNIPEND
