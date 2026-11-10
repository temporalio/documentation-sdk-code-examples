# @@@SNIPSTART ruby-cloud-run-dockerfile
FROM ruby:3.3-slim

WORKDIR /app
RUN gem install temporalio --no-document
COPY *.rb ./

CMD ["ruby", "worker.rb"]
# @@@SNIPEND
