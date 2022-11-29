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

  @Input() selectedUser: UserInfo = null;
  @Input() myName: string = null;

  @Output() deletedUserEmitter = new EventEmitter();

  canIEditUser = false;
  newPassword = '';
  passwordHintColor = 'red';
  editableSelectedUser = new UserInfo();

  hasUsernameChanged = false;
  hasDisplayNameChanged = false;
  hasEmailChanged = false;
  hasUserActiveChanged = false;


  constructor(private userApi: UserApiService, private dialog: DialogService, private createDialog: MatDialog) {
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.selectedUser) {
      this.editableSelectedUser = Object.assign({}, this.selectedUser);
      if (this.myName === this.selectedUser.name) {
        this.canIEditUser = true;
      } else {
        this.userApi.hasSuperPrivileges(this.myName)
          .then((bool) => {
            this.canIEditUser = bool;
          });
      }
    }
  }

  onUserDelete() {
    this.deletedUserEmitter.emit(this.selectedUser);
  }

  onUpdate() {
    console.dir(this.selectedUser);
    this.dialog.openLoadingIndicator(this.userApi.updateUser(this.selectedUser),
      'Updating User');
  }

  onUpdateUsername() {
    const updatedUser: UserInfo = Object.assign(this.selectedUser);
    updatedUser.name = this.editableSelectedUser.name;
    this.dialog.openLoadingIndicator(this.userApi.updateUser(updatedUser)
        .then(() => {
          this.selectedUser.name = this.editableSelectedUser.name;
          this.hasUsernameChanged = false;
        }),
      'Updating Users Username');
  }

  onUpdateDisplayName() {
    const updatedUser: UserInfo = Object.assign(this.selectedUser);
    updatedUser.displayName = this.editableSelectedUser.displayName;

    this.dialog.openLoadingIndicator(this.userApi.updateUser(updatedUser)
        .then(() => {
          this.selectedUser.displayName = this.editableSelectedUser.displayName;
          this.hasDisplayNameChanged = false;
        }),
      'Updating Users Display Name');
  }

  onUpdateEmail() {
    const updatedUser: UserInfo = Object.assign(this.selectedUser);
    updatedUser.email = this.editableSelectedUser.email;
    this.dialog.openLoadingIndicator(this.userApi.updateUser(updatedUser)
        .then(() => {
          this.selectedUser.email = this.editableSelectedUser.email;
          this.hasEmailChanged = false;
        }),
      'Updating Users Email');
  }

  onUpdatePassword() {
    this.dialog.openLoadingIndicator(this.userApi.setPassword(this.selectedUser.name, this.newPassword)
        .then(() => {
          this.newPassword = '';
        }),
      'Updating Password');
  }

  onUpdateActive() {
    const updatedUser: UserInfo = Object.assign(this.selectedUser);
    updatedUser.active = this.editableSelectedUser.active;
    this.dialog.openLoadingIndicator(this.userApi.updateUser(updatedUser)
      .then(() => {
        this.selectedUser.active = this.editableSelectedUser.active;
        this.hasUserActiveChanged = false;
      }),
      'Updating User Activation');
  }

  setPasswordHintColor() {
    if (this.newPassword.length < 8) {
      this.passwordHintColor = 'red';
    } else if (this.newPassword.length >= 8 && this.newPassword.length < 10) {
      this.passwordHintColor = 'orange';
    } else if (this.newPassword.length >= 10 && this.newPassword.length < 12) {
      this.passwordHintColor = 'yellow';
    } else if (this.newPassword.length >= 12) {
      this.passwordHintColor = 'green';
    }
  }

  usernameHasChanged() {
    let toCheck;
    if (this.selectedUser.name === null) {
      toCheck =  '';
    } else {
      toCheck = this.selectedUser.name;
    }
    this.hasUsernameChanged = toCheck !== this.editableSelectedUser.name;
  }

  displayNameHasChanged() {
    let toCheck;
    if (this.selectedUser.displayName === null) {
      toCheck =  '';
    } else {
      toCheck = this.selectedUser.displayName;
    }
    this.hasDisplayNameChanged = toCheck !== this.editableSelectedUser.displayName;
  }

  emailHasChanged() {
    let toCheck;
    if (this.selectedUser.email === null) {
      toCheck =  '';
    } else {
      toCheck = this.selectedUser.email;
    }
    this.hasEmailChanged = toCheck !== this.editableSelectedUser.email;
  }

  userActiveHasChanged() {
    if (this.selectedUser.active === this.editableSelectedUser.active) {
      console.log('Active has not changed from ' + this.selectedUser.active + ' to ' + this.editableSelectedUser.active);
      this.hasUserActiveChanged = false;
    } else {
      console.log('Active has changed from ' + this.selectedUser.active + ' to ' + this.editableSelectedUser.active);
      this.hasUserActiveChanged = true;
    }
  }

  changePasswordBtnClicked() {
    this.createDialog.open(PasswordDialogComponent, {
      data: {
        password: this.newPassword,
      }
    })
      .afterClosed()
      .subscribe((password) => {
        if (this.newPassword === password) {
          this.onUpdatePassword();
        }
      });
  }
}
