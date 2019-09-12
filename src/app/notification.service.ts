import {Injectable} from '@angular/core';
import {MatSnackBar} from '@angular/material';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor(private snack: MatSnackBar) {
  }

  info(message: string) {
    this.snack.open(message, 'OK', {duration: 3000});
  }

  error(message: string) {
    this.snack.open(message, 'OK');
  }
}
