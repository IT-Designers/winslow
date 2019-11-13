import {Component, Input, OnInit} from '@angular/core';
import {EnvSettingsApiService} from '../api/env-settings-api.service';
import {LongLoadingDetector} from '../long-loading-detector';

@Component({
  selector: 'app-system-cfg-env',
  templateUrl: './system-cfg-env.component.html',
  styleUrls: ['./system-cfg-env.component.css']
})
export class SystemCfgEnvComponent implements OnInit {

  // env cache
  environmentVariables: Map<string, [boolean, string]> = null;
  defaultEnvironmentVariablesValue = new Map<string, string>();
  envSubmitValue: any = null;

  longLoadingValue = new LongLoadingDetector();
  longLoadingExternallySet = false;
  loadError = null;

  constructor(private api: EnvSettingsApiService) {
  }

  ngOnInit() {
    this.longLoadingValue.increase();
    this.api.getGlobalEnvironmentVariables()
      .then(result => {
        this.defaultEnvironmentVariablesValue = result;
      })
      .catch(error => this.loadError = error)
      .finally(() => this.longLoadingValue.decrease());
  }

  @Input()
  set longLoading(longLoading: LongLoadingDetector) {
    this.longLoadingValue = longLoading;
    this.longLoadingExternallySet = true;
  }
}
