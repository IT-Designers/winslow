import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {FileBrowseDialog} from '../file-browse-dialog/file-browse-dialog.component';
import {MatDialog} from '@angular/material';

@Component({
  selector: 'app-env-variables',
  templateUrl: './env-variables.component.html',
  styleUrls: ['./env-variables.component.css']
})
export class EnvVariablesComponent implements OnInit {

  environmentVariables: Map<string, [boolean, string]> = null;
  defaultEnvironmentVariablesValue = new Map<string, string>();
  formGroupEnv: FormGroup = null;

  @Output() private valid = new EventEmitter<boolean>();
  @Output() private value = new EventEmitter<any>();
  @Output() private hasChanges = new EventEmitter<boolean>();

  constructor(private dialog: MatDialog) {
    this.initFormGroup();
  }

  ngOnInit() {
  }

  private initFormGroup() {
    this.formGroupEnv = new FormGroup({});
    this.formGroupEnv.valueChanges.subscribe(value => this.value.emit(value));
  }

  @Input()
  set defaults(defaults: Map<string, string>) {
    this.defaultEnvironmentVariablesValue = defaults;
    if (defaults != null) {
      defaults.forEach((value, key) => this.setEnvValue(key, value));
      setTimeout(() => {
        this.formGroupEnv.markAllAsTouched();
        this.updateValid();
      });
    }
  }

  setEnvValue(key: string, value: string) {
    this.prepareEnvFormControl(key, value);
    if (this.environmentVariables == null) {
      this.environmentVariables = new Map();
    }
    const current = this.environmentVariables.get(key);
    if (current != null) {
      this.environmentVariables.set(key, [current[0], value]);
    } else {
      this.environmentVariables.set(key, [false, value]);
    }
  }

  @Input()
  set env(env: Map<string, [boolean, string]>) {
    this.initFormGroup();
    this.environmentVariables = env;
    setTimeout(() => {
      this.formGroupEnv.markAllAsTouched();
      this.updateValid();
    });
  }

  @Input()
  set required(keys: string[]) {
    if (keys != null) {
      keys.forEach(key => this.setEnvRequired(key));
    }
  }

  setEnvRequired(key: string) {
    this.prepareEnvFormControl(key, null);
    if (this.environmentVariables == null) {
      this.environmentVariables = new Map();
    }
    const value = this.environmentVariables.get(key);
    if (value != null) {
      this.environmentVariables.set(key, [true, value[1]]);
    } else {
      this.environmentVariables.set(key, [true, null]);
    }
  }

  prepareEnvFormControl(key: string, value: string) {
    const control = this.formGroupEnv.get(key);
    if (control == null) {
      this.formGroupEnv.setControl(key, new FormControl(value));
    } else {
      control.setValue(value);
      control.updateValueAndValidity();
    }
    this.updateValid();
  }
  updateValid() {
    this.valid.emit(this.isValid());
    this.hasChanges.emit(this.lookForChanges());
  }

  isValid(): boolean {
    return this.formGroupEnv && this.formGroupEnv.valid;
  }

  private lookForChanges() {
    let changesDetected = this.environmentVariables != null && Object.keys(this.formGroupEnv.value).length !== this.environmentVariables.size;
    if (!changesDetected && this.environmentVariables != null) {
      for (const key of Object.keys(this.formGroupEnv.value)) {
        if (!this.environmentVariables.has(key) || this.environmentVariables.get(key)[1] !== this.formGroupEnv.value[key]) {
          changesDetected = true;
          break;
        }
      }
    }
    return changesDetected;
  }

  browseForValue(valueReceiver: HTMLInputElement) {
    this.dialog.open(FileBrowseDialog, {
      data: {
        preselectedPath: valueReceiver.value.trim().length > 0 ? valueReceiver.value.trim() : null
      }
    })
      .afterClosed()
      .toPromise()
      .then(result => {
        if (result) {
          valueReceiver.value = result;
        }
      });
  }

  submit(name: HTMLInputElement, value: HTMLInputElement) {
    this.setEnvValue(name.value.trim(), value.value.trim());
    name.value = null;
    value.value = null;
    name.focus();
  }
}
