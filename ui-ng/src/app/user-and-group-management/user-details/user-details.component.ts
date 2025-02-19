import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {UserApiService} from '../../api/user-api.service';
import {DialogService} from '../../dialog.service';
import {MatDialog} from '@angular/material/dialog';
import {PasswordDialogComponent} from '../password-dialog/password-dialog.component';
import {UserInfo} from "../../api/winslow-api";

@Component({
  selector: 'app-user-details',
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.css']
})
export class UserDetailsComponent implements OnInit, OnChanges {

  @Input() selectedUser?: UserInfo;   // Object should remain constant
  @Input() myName?: string;

  @Output() deletedUserEmitter = new EventEmitter();

  canIEditUser = false;
  newPassword = '';
  editableSelectedUser: UserInfo = {active: false, displayName: "", email: "", name: "", password: [""]};

  hasAnythingChanged = false;

  constructor(private userApi: UserApiService, private dialog: DialogService, private createDialog: MatDialog) {
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.selectedUser && this.myName) {
      this.editableSelectedUser = Object.assign({}, this.selectedUser);
      this.userApi.hasSuperPrivileges(this.myName)
        .then((bool) => {
          if (bool) {
            this.canIEditUser = bool;
          } else {
            this.canIEditUser = this.myName === this.selectedUser?.name;
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
    const user = this.selectedUser;
    if (user == undefined) {
      this.dialog.error("Cannot update password: No user selected.");
      return
    }
    this.dialog.openLoadingIndicator(this.userApi.setPassword(user.name, password)
        .then(() => {
          this.newPassword = '';
          user.password?.push('********');
          this.selectedUser = user;
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
      data: {}
    })
      .afterClosed()
      .subscribe((password) => {
        if (password) {
          this.onUpdatePassword(password);
        }
      });
  }
}
