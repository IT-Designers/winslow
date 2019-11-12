import { Component, OnInit } from '@angular/core';

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

  constructor() { }

  ngOnInit() {
  }
}
