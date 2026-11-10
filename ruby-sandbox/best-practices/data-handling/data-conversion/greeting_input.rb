# frozen_string_literal: true

require_relative 'active_model_json_support'

# An ActiveModel class that a Workflow or Activity can take as an argument
# or return as a result.
class GreetingInput
  include ActiveModel::API
  include ActiveModel::Attributes
  include ActiveModelJSONSupport

  attribute :name, :string
  attribute :count, :integer
end
