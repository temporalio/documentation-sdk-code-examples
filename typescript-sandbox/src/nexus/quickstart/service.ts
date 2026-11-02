// @@@SNIPSTART typescript-nexus-quickstart-service
import * as nexus from 'nexus-rpc';

export interface MyInput {
  name: string;
}

export const sayHelloService = nexus.service('say-hello', {
  sayHello: nexus.operation<MyInput, string>(),
});
// @@@SNIPEND
