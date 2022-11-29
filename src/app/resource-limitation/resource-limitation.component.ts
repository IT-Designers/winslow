import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {IResourceLimitationExt} from '../api/project-api.service';


@Component({
  selector: 'app-resource-limitation',
  templateUrl: './resource-limitation.component.html',
  styleUrls: ['./resource-limitation.component.css']
})
export class ResourceLimitationComponent implements OnInit {

  @Input() title: string;
  @Output('limit') change = new EventEmitter<IResourceLimitationExt>();

  local?: IResourceLimitationExt;
  remote?: IResourceLimitationExt;

  constructor() {
  }

  ngOnInit(): void {
  }

  @Input()
  set limit(limit: IResourceLimitationExt) {
    this.local = limit != null ? new IResourceLimitationExt(limit) : null;
    this.remote = limit;
  }

  maybeInitLocal(checked: boolean) {
    if (checked) {
      this.local = new IResourceLimitationExt(this.remote);
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
    return IResourceLimitationExt.equals(this.local, this.remote);
  }
}
