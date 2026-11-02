import * as nexus from 'nexus-rpc';

export const echoService = nexus.service('echo', {
  echo: nexus.operation<string, string>(),
});

export const echoServiceHandler = nexus.serviceHandler(echoService, {
  async echo(_ctx, input) {
    return input;
  },
});
