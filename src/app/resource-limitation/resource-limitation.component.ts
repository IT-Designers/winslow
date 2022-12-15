import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {similarResourceLimitation} from '../api/project-api.service';
import {ResourceLimitation} from '../api/winslow-api';


@Component({
  selector: 'app-resource-limitation',
  templateUrl: './resource-limitation.component.html',
  styleUrls: ['./resource-limitation.component.css']
})
export class ResourceLimitationComponent implements OnInit {

  @Input() title: string;
  @Output('limit') change = new EventEmitter<ResourceLimitation>();

  local?: ResourceLimitation;
  remote?: ResourceLimitation;

  constructor() {
  }

  ngOnInit(): void {
  }

  @Input()
  set limit(limit: ResourceLimitation) {
    this.local = limit != null ? new ResourceLimitation(limit) : null;
    this.remote = limit;
  }

  maybeInitLocal(checked: boolean) {
    if (checked) {
      this.local = new ResourceLimitation(this.remote);
    } else {
      this.local = null;
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
