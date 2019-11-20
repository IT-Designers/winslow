import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import {FileBrowseDialog} from '../file-browse-dialog/file-browse-dialog.component';
import {MatDialog} from '@angular/material';
import {EnvVariable} from '../api/project-api.service';

@Component({
  selector: 'app-env-variables',
  templateUrl: './env-variables.component.html',
  styleUrls: ['./env-variables.component.css']
})
export class EnvVariablesComponent implements OnInit {

  keys: Set<string> = new Set();

  environmentVariables: Map<string, EnvVariable> = null;
  requiredEnvVariables: Set<string> = new Set();
  defaultValues: Map<string, string> = null;

  formGroupEnv: FormGroup = null;

  @Output() private valid = new EventEmitter<boolean>();
  @Output() private value = new EventEmitter<any>();
  @Output() private hasChanges = new EventEmitter<boolean>();

  constructor(private dialog: MatDialog) {
  }

  ngOnInit() {
    this.rebuildKeys();
    this.rebuildEnvControl();
  }

  private rebuildKeys() {
    const keys = new Set<string>();
    if (this.environmentVariables != null) {
      this.environmentVariables.forEach((value, key) => keys.add(key));
    }
    if (this.requiredEnvVariables != null) {
      this.requiredEnvVariables.forEach(key => keys.add(key));
    }
    if (this.defaultValues != null) {
      this.defaultValues.forEach((value, key) => keys.add(key));
    }
    this.keys = keys;
  }

  private rebuildEnvControl() {
    this.formGroupEnv = new FormGroup({});
    this.formGroupEnv.valueChanges.subscribe(value => this.value.emit(value));
    if (this.keys != null) {
      this.keys.forEach(key => {
        this.prepareEnvFormControl(key, this.valueOf(key));
      });
    }
  }

  private valueOf(key: string) {
    const variable = this.environmentVariables != null ? this.environmentVariables.get(key) : null;
    if (variable != null) {
      return variable.value;
    } else {
      return this.defaultValues != null ? this.defaultValues.get(key) : null;
    }
  }

  @Input()
  set defaults(defaults: Map<string, string>) {
    this.defaultValues = defaults;
    this.rebuildKeys();
    this.rebuildEnvControl();
    setTimeout(() => {
      this.formGroupEnv.markAllAsTouched();
      this.updateValid();
    });
  }

  @Input()
  set env(env: Map<string, EnvVariable>) {
    this.environmentVariables = env;
    this.rebuildKeys();
    this.rebuildEnvControl();
    setTimeout(() => {
      this.formGroupEnv.markAllAsTouched();
      this.updateValid();
    });
  }

  @Input()
  set required(keys: string[]) {
    const set = new Set<string>();
    if (keys != null) {
      keys.forEach(key => set.add(key));
    }
    this.requiredEnvVariables = set;
    this.rebuildKeys();
    this.rebuildEnvControl();
    setTimeout(() => {
      this.formGroupEnv.markAllAsTouched();
      this.updateValid();
    });
  }

  prepareEnvFormControl(key: string, value: string) {
    const control = this.formGroupEnv.get(key);
    if (control == null) {
      this.formGroupEnv.setControl(key, new FormControl(value));
    } else {
      control.setValue(value);
      control.updateValueAndValidity();
    }
  }

  updateValid() {
    this.valid.emit(this.isValid());
    this.hasChanges.emit(this.lookForChanges());
  }

  isValid(): boolean {
    return this.formGroupEnv && this.formGroupEnv.valid;
  }

  private lookForChanges() {
    const defaults = this.defaultValues;
    let changesDetected = defaults != null && Object.keys(this.formGroupEnv.value).length !== defaults.size;
    if (!changesDetected && defaults != null) {
      for (const key of Object.keys(this.formGroupEnv.value)) {
        if (!defaults.has(key) || defaults.get(key) !== this.formGroupEnv.value[key]) {
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

  add(name: HTMLInputElement, value: HTMLInputElement) {
    this.setEnvValue(name.value.trim(), value.value.trim());
    this.updateValid();
    name.value = null;
    value.value = null;
    name.focus();
  }

  private setEnvValue(key: string, value: string) {
    this.prepareEnvFormControl(key, value);
    if (this.environmentVariables == null) {
      this.environmentVariables = new Map();
    }
    let current = this.environmentVariables.get(key);
    if (current == null) {
      current = new EnvVariable();
      this.environmentVariables.set(key, current);
    }
    current.value = value;
    if (!this.keys.has(key)) {
      setTimeout(() => this.keys.add(key));
    }
  }

  delete(key: string) {
    if (this.environmentVariables != null && this.environmentVariables.has(key) && this.environmentVariables.get(key).valueInherited != null) {
      this.environmentVariables.get(key).value = null;
      this.formGroupEnv.get(key).setValue(null);
    } else {
      if (this.environmentVariables != null) {
        this.environmentVariables.delete(key);
      }
      this.formGroupEnv.removeControl(key);
      this.keys.delete(key);
    }
    this.formGroupEnv.markAllAsTouched();
    this.updateValid();
  }

  valueOrInherited(key: string, value: string) {
    if (value != null && value.length === 0) {
      value = null;
    }

    const env = this.environmentVariables != null ? this.environmentVariables.get(key) : null;
    const envValue = env != null ? env.value : (this.defaultValues != null ? this.defaultValues.get(key) : null);
    const envInherited = env != null ? env.valueInherited : null;

    const result = (value !== envValue || envInherited == null ? envValue : envInherited);
    console.log(result);
    return result;
  }
}
