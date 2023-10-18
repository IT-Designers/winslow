import {Injectable} from '@angular/core';
import Swal, {SweetAlertOptions, SweetAlertResult} from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class DialogService {

  constructor() {
  }

  private fireWithSuccessNotification(options: SweetAlertOptions): Promise<SweetAlertResult> {
    return Swal
      .fire(options)
      .then(result => {
        if (result.value) {
          return Swal.fire({
            icon: 'success',
            timer: 700,
          });
        }
        return result;
      });
  }

  private errorCatcher(promise: Promise<void>): Promise<boolean> {
    return promise
      .then(() => true)
      .catch(err => {
        Swal.clickCancel();
        if (err && err.error && err.error.text) {
          Swal.fire({
            icon: 'error',
            html: `<pre style="overflow: auto">${err.error.text}</pre>`,
          });
        } else {
          Swal.fire({
            icon: 'error',
            text: err != undefined ? JSON.stringify(err) : undefined,
          });
        }
        return false;
      });
  }

  private preConfirmPromiseWithErrorCatcher<T>(promise: (value: T) => Promise<void>): (v: T) => Promise<boolean> {
    return v => this.errorCatcher(promise(v));
  }

  openLoadingIndicator(toExecute: Promise<any>, text?: string, withSuccessNotification = true, showLoadingImmediately = false) {
    const options: SweetAlertOptions = {
      icon: 'info',
      titleText: 'Please wait',
      text,
      showConfirmButton: true,
      showCancelButton: false,
      confirmButtonText: 'Start',
      showLoaderOnConfirm: true,
      preConfirm: this.preConfirmPromiseWithErrorCatcher(() => toExecute),
    };

    const state = {showed: false};

    // try to prevent the loading popup from being displayed if it would only be visible for a really short moment
    toExecute.finally(() => this.show(options, withSuccessNotification, state, true));
    if (showLoadingImmediately) {
      this.show(options, withSuccessNotification, state);
    } else {
      setTimeout(() => this.show(options, withSuccessNotification, state), 500);
    }
  }

  private show(options: SweetAlertOptions, withSuccessNotification: boolean, state: {showed: boolean}, done = false) {
    if (state && !state.showed) {
      state.showed = true;
      if (!done || withSuccessNotification) {
        withSuccessNotification ? this.fireWithSuccessNotification(options) : Swal.fire(options);
        Swal.clickConfirm();
      }
    }
  }

  openAreYouSure(text: string, onSure: () => Promise<void>) {
    return this.fireWithSuccessNotification({
      icon: 'warning',
      titleText: 'Are you sure?',
      text,
      showConfirmButton: true,
      showCancelButton: true,
      confirmButtonText: 'Do it!',
      confirmButtonColor: '#FF0000',
      cancelButtonText: 'Not today',
      showLoaderOnConfirm: true,
      preConfirm: this.preConfirmPromiseWithErrorCatcher(onSure)
    });
  }

  createAThing(thing: string, placeholder: string, action: (input: string) => Promise<void>) {
    return this.fireWithSuccessNotification({
      title: 'Create a ' + thing,
      input: 'text',
      inputPlaceholder: placeholder,
      showLoaderOnConfirm: true,
      focusConfirm: false,
      preConfirm: this.preConfirmPromiseWithErrorCatcher(action),
    });
  }

  multiInput(title: string, inputs: InputDefinition[], action: (input: string[]) => Promise<void>) {
    const prefix = 'swal-multi-input-' + Math.random() + '-';
    let html = '';
    for (let i = 0; i < inputs.length; ++i) {
      const id = prefix + i;

      html += `<label for="${id}">${inputs[i].getTitle()}</label>`;
      html += `<input  id="${id}" class="swal2-input"
                placeholder="${inputs[i].getPlaceholder()}" value="${inputs[i].getValue()}">`;
    }
    return this.fireWithSuccessNotification({
      title,
      html,
      showLoaderOnConfirm: true,
      focusConfirm: false,
      allowEnterKey: true,
      preConfirm: this.preConfirmPromiseWithErrorCatcher(() => {
        const values = [];
        for (let i = 0; i < inputs.length; ++i) {
          values.push((document.getElementById(prefix + i) as HTMLInputElement)?.value);
        }
        return action(values);
      }),
    });
  }

  renameAThing(thing: string, placeholder: string, action: (input: string) => Promise<void>) {
    return this.fireWithSuccessNotification({
      title: 'Rename ' + thing,
      input: 'text',
      inputPlaceholder: placeholder,
      inputValue: thing,
      showLoaderOnConfirm: true,
      preConfirm: this.preConfirmPromiseWithErrorCatcher(action),
    });
  }

  error(text: string) {
    Swal.fire({
      icon: 'error',
      titleText: 'An error occurred',
      text,
      showConfirmButton: true,
      showCancelButton: false,
    });
  }

  info(text: string, title = 'Information') {
    Swal.fire({
      icon: 'info',
      titleText: title,
      text,
      showConfirmButton: true,
      showCancelButton: false,
    });
  }

  image(url: string) {
    Swal.fire({
        imageUrl: url,
        imageAlt: 'Project Icon',
        imageHeight: '15vh',
        imageWidth: 'auto'
      });
  }

}

export class InputDefinition {
  title: string;
  placeholder?: string;
  value?: string;

  constructor(title: string, placeholder?: string, value?: string) {
    this.title = title;
    this.placeholder = placeholder;
    this.value = value;
  }

  getTitle(): string {
    return this.title;
  }

  getPlaceholder(): string {
    return this.placeholder ?? '';
  }

  getValue(): string {
    return this.value ?? '';
  }
}
