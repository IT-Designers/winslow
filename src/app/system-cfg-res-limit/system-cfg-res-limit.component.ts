import {Component, Input, OnInit} from '@angular/core';
import {LongLoadingDetector} from '../long-loading-detector';
import {SettingsApiService} from '../api/settings-api.service';
import {DialogService} from '../dialog.service';
import {ResourceLimitationExt} from '../api/project-api.service';

@Component({
  selector: 'app-system-cfg-res-limit',
  templateUrl: './system-cfg-res-limit.component.html',
  styleUrls: ['./system-cfg-res-limit.component.css']
})
export class SystemCfgResLimitComponent implements OnInit {


  longLoadingValue = new LongLoadingDetector();
  longLoadingExternallySet = false;
  loadError = null;

  limitServer = ResourceLimitationExt.create();
  limitUpdate = ResourceLimitationExt.create();


  constructor(private api: SettingsApiService, private dialog: DialogService) {
  }

  ngOnInit(): void {
    this.api
      .getUserResourceLimitation()
      .then(userLimits => this.updateLimit(userLimits));
  }


  private updateLimit(serverLimit: ResourceLimitationExt) {
    this.limitServer = new ResourceLimitationExt(serverLimit);
    this.limitUpdate = new ResourceLimitationExt(serverLimit);
  }

  @Input()
  set longLoading(longLoading: LongLoadingDetector) {
    this.longLoadingValue = longLoading;
    this.longLoadingExternallySet = true;
  }

  save() {
    const prevLimit = this.limitServer;
    this.limitServer = ResourceLimitationExt.create();
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
