import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {similarResourceLimitation} from '../api/project-api.service';
import {ResourceLimitation} from '../api/winslow-api';


@Component({
  selector: 'app-resource-limitation',
  templateUrl: './resource-limitation.component.html',
  styleUrls: ['./resource-limitation.component.css']
})
export class ResourceLimitationComponent implements OnInit {

  @Input() title: string = "Resource Limitation";
  @Output('limit') change = new EventEmitter<ResourceLimitation>();

  local?: ResourceLimitation;
  remote?: ResourceLimitation;

  constructor() {
  }

  ngOnInit(): void {
  }

  @Input()
  set limit(limit: ResourceLimitation | undefined) {
    this.local = limit != undefined ? new ResourceLimitation(limit) : undefined;
    this.remote = limit;
  }

  maybeInitLocal(checked: boolean) {
    if (checked && this.remote) {
      this.local = new ResourceLimitation(this.remote);
    } else {
      this.local = undefined;
    }
  }

  submitLocal() {
    this.change.emit(this.local);
  }

  toNumberOrNull(text: string) {
    const num = Number(text);
    if (num <= 0) {
      return null;
    } else {
      return num;
    }
  }

  localRemoteEq() {
    return similarResourceLimitation(this.local, this.remote);
  }
}
