import {EventEmitter} from '@angular/core';
import {RxStompService} from '@stomp/ng2-stompjs';
import {Message} from '@stomp/stompjs';
import {ChangeEvent, ChangeType} from './api.service';
import {Subscription} from 'rxjs';

export class SubscriptionHandler<T, V> {

  private cache: Map<T, V> = new Map();
  private subscriptions: EventEmitter<ChangeEvent<T, V>> = new EventEmitter(true);

  constructor(private rxStompService: RxStompService, private path: string, private mapper?: (V) => V) {
    this.initSubscription();
  }

  private initSubscription() {
    this.subscriptions.subscribe((event: ChangeEvent<T, V>) => {
      if (event.type === ChangeType.DELETE) {
        this.cache.delete(event.identifier);
      } else {
        this.cache.set(event.identifier, event.value);
      }
    });
    this.rxStompService.watch(this.path).subscribe((message: Message) => {
      const events: ChangeEvent<T, V>[] = JSON.parse(message.body);
      if (this.mapper != null) {
        events.forEach(event => event.value = this.mapper(event.value));
      }
      events.forEach(event => this.subscriptions.emit(event));
    });
  }

  public subscribe(handler: (id: T, value?: V) => void): Subscription {
    const subscription = this.subscriptions.subscribe((event: ChangeEvent<T, V>) => {
      handler(event.identifier, event.value);
    });
    for (const entry of this.cache.entries()) {
      handler(entry[0], entry[1]);
    }
    return subscription;
  }

  public getCached(): IterableIterator<V> {
    return this.cache.values();
  }

}
