import {Component, Input, OnInit} from '@angular/core';
import {LongLoadingDetector} from '../long-loading-detector';
import {SettingsApiService, UserResourceLimitation} from '../api/settings-api.service';
import {DialogService} from '../dialog.service';

@Component({
  selector: 'app-system-cfg-res-limit',
  templateUrl: './system-cfg-res-limit.component.html',
  styleUrls: ['./system-cfg-res-limit.component.css']
})
export class SystemCfgResLimitComponent implements OnInit {


  longLoadingValue = new LongLoadingDetector();
  longLoadingExternallySet = false;
  loadError = null;

  limitServer = new UserResourceLimitation();
  limitUpdate = new UserResourceLimitation();


  constructor(private api: SettingsApiService, private dialog: DialogService) {
  }

  ngOnInit(): void {
    this.api
      .getUserResourceLimitation()
      .then(userLimits => this.updateLimit(userLimits));
  }


  private updateLimit(serverLimit: UserResourceLimitation) {
    this.limitServer = new UserResourceLimitation(serverLimit);
    this.limitUpdate = new UserResourceLimitation(serverLimit);
  }

  @Input()
  set longLoading(longLoading: LongLoadingDetector) {
    this.longLoadingValue = longLoading;
    this.longLoadingExternallySet = true;
  }

  save() {
    const prevLimit = this.limitServer;
    this.limitServer = new UserResourceLimitation();
    this.dialog.openLoadingIndicator(
      this.api
        .setUserResourceLimitation(this.limitUpdate)
        .then(limit => this.updateLimit(limit))
        .catch(e => {
          this.limitServer = prevLimit;
          return e;
        }),
      `Submitting new set of global user resource limitations`
    );
  }

  cpuLimitChanged($event: number) {
    console.log('new limit for cpu: ' + $event);
    this.limitUpdate.cpu = $event;
  }

  memLimitChanged($event: number) {
    console.log('new limit for mem: ' + $event);
    this.limitUpdate.mem = $event;
  }

  gpuLimitChanged($event: number) {
    console.log('new limit for gpu: ' + $event);
    this.limitUpdate.gpu = $event;
  }
}
