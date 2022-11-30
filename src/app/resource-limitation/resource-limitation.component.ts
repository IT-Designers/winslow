import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {ResourceLimitationExt} from '../api/project-api.service';


@Component({
  selector: 'app-resource-limitation',
  templateUrl: './resource-limitation.component.html',
  styleUrls: ['./resource-limitation.component.css']
})
export class ResourceLimitationComponent implements OnInit {

  @Input() title: string;
  @Output('limit') change = new EventEmitter<ResourceLimitationExt>();

  local?: ResourceLimitationExt;
  remote?: ResourceLimitationExt;

  constructor() {
  }

  ngOnInit(): void {
  }

  @Input()
  set limit(limit: ResourceLimitationExt) {
    this.local = limit != null ? new ResourceLimitationExt(limit) : null;
    this.remote = limit;
  }

  maybeInitLocal(checked: boolean) {
    if (checked) {
      this.local = new ResourceLimitationExt(this.remote);
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
    return ResourceLimitationExt.equals(this.local, this.remote);
  }
}
