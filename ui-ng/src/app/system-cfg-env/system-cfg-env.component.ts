import {Component, Input, OnInit} from '@angular/core';
import {SettingsApiService} from '../api/settings-api.service';
import {LongLoadingDetector} from '../long-loading-detector';
import {DialogService} from '../dialog.service';
import {EnvVariable} from "../api/winslow-api";

@Component({
  selector: 'app-system-cfg-env',
  templateUrl: './system-cfg-env.component.html',
  styleUrls: ['./system-cfg-env.component.css']
})
export class SystemCfgEnvComponent implements OnInit {

  // env cache
  environmentVariables?: Map<string, EnvVariable>;
  defaultEnvironmentVariablesValue = new Map<string, string>();
  envSubmitValue: any = null;

  longLoadingValue = new LongLoadingDetector();
  longLoadingExternallySet = false;
  loadError?: Error;

  constructor(private api: SettingsApiService, private dialog: DialogService) {
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

  save() {
    this.dialog.openLoadingIndicator(
      this.api
        .setGlobalEnvironmentVariables(this.envSubmitValue)
        .then(r => {
          const defaults = new Map();
          Object.keys(this.envSubmitValue).forEach(key => defaults.set(key, this.envSubmitValue[key]));
          this.defaultEnvironmentVariablesValue = defaults;
        }),
      `Submitting new set of global environment variables`
    );
  }
}
