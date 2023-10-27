import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {UntypedFormControl, UntypedFormGroup, Validators} from '@angular/forms';
import {FileBrowseDialog} from '../file-browse-dialog/file-browse-dialog.component';
import {MatDialog} from '@angular/material/dialog';
import {EnvVariable} from '../api/winslow-api';

@Component({
  selector: 'app-env-variables',
  templateUrl: './env-variables.component.html',
  styleUrls: ['./env-variables.component.css']
})
export class EnvVariablesComponent implements OnInit {

  keys: Set<string> = new Set();

  environmentVariables?: Map<string, EnvVariable>;
  requiredEnvVariables: Set<string> = new Set();
  defaultValues?: Map<string, string>;

  formGroupEnv!: UntypedFormGroup;

  // caches for angular
  undoTarget: any = {};
  formControls: any = {};
  values: any = {};

  @Output() private valid = new EventEmitter<boolean>();
  @Output() private value = new EventEmitter<any>();
  @Output() private hasChanges = new EventEmitter<boolean>();

  constructor(private dialog: MatDialog) {
  }

  ngOnInit() {
    this.rebuildKeys();
    this.rebuildEnvControl();
    this.updateValid();
  }

  private rebuildKeys() {
    const keys = new Set<string>();
    if (this.environmentVariables != undefined) {
      this.environmentVariables.forEach((value, key) => keys.add(key));
    }
    if (this.requiredEnvVariables != undefined) {
      this.requiredEnvVariables.forEach(key => keys.add(key));
    }
    if (this.defaultValues != undefined) {
      this.defaultValues.forEach((value, key) => keys.add(key));
    }
    this.keys = keys;
  }

  private rebuildEnvControl() {
    this.formGroupEnv = new UntypedFormGroup({});
    this.formGroupEnv.valueChanges.subscribe(value => this.value.emit(value));
    if (this.keys != undefined) {
      const controls : {[key: string]: any}= {};
      this.keys.forEach(key => {
        this.prepareEnvFormControl(key, this.valueOf(key));
        controls[key] = this.formGroupEnv.get(key);
      });
      this.formControls = controls;
    }
  }

  valueOf(key: string) {
    const variable = this.environmentVariables != undefined ? this.environmentVariables.get(key) : undefined;
    if (variable != undefined) {
      return this.values[key] = variable.value;
    } else {
      return this.values[key] = (this.defaultValues != undefined ? this.defaultValues.get(key) : undefined);
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
  set env(env: Map<string, EnvVariable> | undefined) {
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
    if (keys != undefined) {
      keys.forEach(key => set.add(key));
    }
    this.requiredEnvVariables = set;
    this.rebuildKeys();
    this.rebuildEnvControl();
    this.updateValid();
    setTimeout(() => {
      this.formGroupEnv.markAllAsTouched();
      this.updateValid();
    });
  }

  prepareEnvFormControl(key: string, value?: string) {
    let control = this.formGroupEnv.get(key);
    if (control == undefined) {
      control = new UntypedFormControl(value);
      this.formGroupEnv.setControl(key, control);
      this.formControls[key] = control;
    } else {
      control.setValue(value);
    }
    if (this.requiredEnvVariables.has(key)) {
      control.setValidators(Validators.required);
    }
    control.updateValueAndValidity();
    this.updateUndoTarget(key);
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
    let changesDetected = defaults != undefined && Object.keys(this.formGroupEnv.value).length !== defaults.size;
    if (!changesDetected && defaults != undefined) {
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
        preselectedPath: valueReceiver.value.trim().length > 0 ? valueReceiver.value.trim() : undefined
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
    name.value = '';
    value.value = '';
    name.focus();
  }

  private setEnvValue(key: string, value: string) {
    this.prepareEnvFormControl(key, value);
    if (this.environmentVariables == undefined) {
      this.environmentVariables = new Map();
    }
    let current = this.environmentVariables.get(key);
    if (current == null) {
      current = new EnvVariable({
        key,
        value,
        valueInherited: undefined
      });
      this.environmentVariables.set(key, current);
    } else {
      current.value = value;
    }
    if (!this.keys.has(key)) {
      this.keys.add(key);
    }
  }

  delete(key: string) {
    if (this.environmentVariables != undefined
      && this.environmentVariables.has(key)
      && this.environmentVariables.get(key)?.valueInherited != undefined) {
      this.formGroupEnv.get(key)?.setValue(undefined);
    } else {
      this.formGroupEnv.removeControl(key);
      this.formControls[key] = undefined;
      this.keys.delete(key);
    }
    this.formGroupEnv.markAllAsTouched();
    this.updateValid();
  }

  valueOrInherited(key: string) {
    const env = this.environmentVariables?.get(key);
    const envValue = env != undefined ? env.value : (this.defaultValues?.get(key));
    const envInherited = env?.valueInherited;

    return envValue != undefined || envInherited == undefined ? envValue : envInherited;
  }

  private updateUndoTarget(key: string) {
    this.undoTarget[key] = this.valueOrInherited(key);
  }
}
