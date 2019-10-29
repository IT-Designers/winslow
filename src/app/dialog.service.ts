import { Injectable } from '@angular/core';
import Swal, {SweetAlertOptions, SweetAlertResult} from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class DialogService {

  constructor() { }

  private fireWithSuccessNotification(options: SweetAlertOptions): Promise<SweetAlertResult> {
    return Swal
      .fire(options)
      .then(result => {
        if (result.value) {
          return Swal.fire({
            type: 'success',
            timer: 1000,
          });
        }
        return result;
      });
  }

  private errorCatcher(promise: Promise<any>): Promise<boolean> {
    return promise
      .catch(err => {
        Swal.clickCancel();
        Swal.fire({
          type: 'error',
          text: err != null ? '' + err : null,
        });
      })
      .then(result => true);
  }

  private preConfirmPromiseWithErrorCatcher(promise: (value: any) => Promise<any>): (r) => Promise<boolean> {
    return v => this.errorCatcher(promise(v));
  }

  openLoadingIndicator(toExecute: Promise<any>, text?: string, withSuccessNotification = true) {
    const options: SweetAlertOptions = {
      type: 'info',
      titleText: 'Please wait',
      text,
      showConfirmButton: true,
      showCancelButton: false,
      confirmButtonText: 'Start',
      showLoaderOnConfirm: true,
      preConfirm: this.preConfirmPromiseWithErrorCatcher(() => toExecute),
    };

    const promiseState = { done: false };
    toExecute.finally(() => promiseState.done = true);

    setTimeout(() => {
      if (!promiseState.done || withSuccessNotification) {
        withSuccessNotification ? this.fireWithSuccessNotification(options) : Swal.fire(options);
        Swal.clickConfirm();
      }
    }, 150); // try to prevent the first popup from being displayed if it would only be visible for a really short moment
  }

  openAreYouSure(text: string, onSure: () => Promise<void>) {
    this.fireWithSuccessNotification({
      type: 'warning',
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
    this.fireWithSuccessNotification({
      title: 'Create a ' + thing,
      input: 'text',
      inputPlaceholder: placeholder,
      showLoaderOnConfirm: true,
      preConfirm: this.preConfirmPromiseWithErrorCatcher(action),
    });
  }

  error(text: string) {
    Swal.fire({
      type: 'error',
      titleText: 'An error occurred',
      text,
      showConfirmButton: true,
      showCancelButton: false,
    });
  }
}
