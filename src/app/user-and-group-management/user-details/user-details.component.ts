import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {UserApiService, UserInfo} from '../../api/user-api.service';
import {DialogService} from '../../dialog.service';
import {MatDialog} from '@angular/material/dialog';
import {PasswordDialogComponent} from '../password-dialog/password-dialog.component';

@Component({
  selector: 'app-user-details',
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.css']
})
export class UserDetailsComponent implements OnInit, OnChanges {

  @Input() selectedUser!: UserInfo;   // Object should remain constant
  @Input() myName!: string;

  @Output() deletedUserEmitter = new EventEmitter();

  canIEditUser = false;
  newPassword = '';
  editableSelectedUser = new UserInfo();

  hasAnythingChanged = false;

  constructor(private userApi: UserApiService, private dialog: DialogService, private createDialog: MatDialog) {
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.selectedUser) {
      this.editableSelectedUser = Object.assign({}, this.selectedUser);
      this.userApi.hasSuperPrivileges(this.myName)
        .then((bool) => {
          if (bool) {
            this.canIEditUser = bool;
          } else {
            this.canIEditUser = this.myName === this.selectedUser.name;
          }
        });
    }
  }

  onUserDelete() {
    this.deletedUserEmitter.emit(this.selectedUser);
  }

  onUpdate() {
    this.dialog.openLoadingIndicator(this.userApi.updateUser(this.editableSelectedUser)
        .then(() => {
          this.selectedUser = Object.assign({}, this.editableSelectedUser);
        }),
      'Updating User');
  }

  onUpdatePassword(password: string) {
    this.dialog.openLoadingIndicator(this.userApi.setPassword(this.selectedUser.name, password)
        .then(() => {
          this.newPassword = '';
          this.selectedUser.password = '********';
        }),
      'Updating Password');
  }

  somethingIsBeingChanged() {
    if (JSON.stringify(this.selectedUser) === JSON.stringify(this.editableSelectedUser)) {
      this.hasAnythingChanged = false;
    } else if (JSON.stringify(this.selectedUser) !== JSON.stringify(this.editableSelectedUser)) {
      this.hasAnythingChanged = true;
    }
  }

  changePasswordBtnClicked() {
    this.createDialog.open(PasswordDialogComponent, {
      data: {
      }
    })
      .afterClosed()
      .subscribe((password) => {
        if (password) {
          this.onUpdatePassword(password);
        }
      });
  }
}
