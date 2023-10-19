import { SubscriptionHandler } from './subscription-handler';
import {RxStompService} from "../rx-stomp.service";

describe('SubscriptionCache', () => {
  it('should create an instance', () => {
    expect(new SubscriptionHandler(new RxStompService(), "")).toBeTruthy();
  });
});
