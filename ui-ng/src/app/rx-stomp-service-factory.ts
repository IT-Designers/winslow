import { RxStompService } from './rx-stomp.service';
import { customRxStompConfig } from './rx-stomp.config';

export function rxStompServiceFactory() {
  const rxStomp = new RxStompService();
  rxStomp.configure(customRxStompConfig);
  rxStomp.activate();
  return rxStomp;
}
