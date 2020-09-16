import {Injectable} from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor(private snack: MatSnackBar) {
  }

  info(message: string) {
    this.snack.open(message, 'OK', {duration: 2_500});
  }

  error(message: string) {
    this.snack.open(message, 'OK', {duration: 10_000});
  }
}
