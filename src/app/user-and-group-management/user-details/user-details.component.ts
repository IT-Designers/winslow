import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {UserApiService, UserInfo} from '../../api/user-api.service';
import {DialogService} from '../../dialog.service';

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



  constructor(private userApi: UserApiService, private dialog: DialogService) { }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.selectedUser) {
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
  onUpdatePassword() {
    this.dialog.openLoadingIndicator(this.userApi.setPassword(this.selectedUser.name, this.newPassword),
      'Updating Password');
  }

}
