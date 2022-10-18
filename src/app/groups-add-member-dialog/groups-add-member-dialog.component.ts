import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {UserApiService} from '../api/user-api.service';
import {RoleApiService} from '../api/role-api.service';

export interface AddMemberData {
  name: string;
  role: string;
}

@Component({
  selector: 'app-groups-add-member-dialog',
  templateUrl: './groups-add-member-dialog.component.html',
  styleUrls: ['./groups-add-member-dialog.component.css']
})
export class GroupsAddMemberDialogComponent implements OnInit {

  allUsers: AddMemberData[];
  displayUsers: AddMemberData[];
  allRoles: string[];
  userSearchInput = '';
  showUsersToggle = false;

  constructor(
    public dialogRef: MatDialogRef<GroupsAddMemberDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AddMemberData,
    private userApi: UserApiService,
    private roleApi: RoleApiService) { }

  ngOnInit(): void {
    this.userApi.getUsers().then((users) => {
      this.allUsers = users;
    });
    this.roleApi.getRoles().then((roles) => this.allRoles = roles);
  }

  onKeyUp() {
    if (this.userSearchInput.length >= 2) {
      this.showUsersToggle = true;
      this.filterFunction();
    } /*else if (this.userSearchInput.length < 2) {
      this.showUsersToggle = false;
      this.filterFunction();
    }*/
    if (this.showUsersToggle) {
      this.filterFunction();
    }
  }

  filterFunction() {
    this.displayUsers = Array.from(this.allUsers);
    if (this.userSearchInput) {
      let i = 0;
      for (const user of this.displayUsers) {
        if (!user.name.includes(this.userSearchInput)) {
          this.displayUsers.splice(i, 1);
        }
        i++;
      }
    }


    /*let filter;
    let divs;
    let i;
    filter = this.userSearchInput.toUpperCase();
    divs = document.getElementsByClassName('user-item-from-dialog');
    for (i = 0; i < divs.length; i++) {
      const txtValue = this.allUsers[i].name;
      /!*let txtValue = divs[i].textContent;
       const substringToRemove = ' Choose Roleperson_add';
       txtValue = txtValue.substring(0, txtValue.length - substringToRemove.length);
       console.log(txtValue);
       console.dir(this.users);*!/
      if (txtValue.toUpperCase().indexOf(filter) > -1) {
        divs[i].style.display = '';
      } else {
        divs[i].style.display = 'none';
      }
    }*/
  }

  roleChanged(role, username) {
    const buttons = document.getElementsByClassName('add-member-round-button') as HTMLCollectionOf<HTMLButtonElement>;
    const userLabels = document.getElementsByClassName('username-label-popup');
    let i = 0;
    while (userLabels[i].innerHTML !== username) {
      i++;
    }
    if (role) {
      buttons[i].disabled = false;
    } else {
      buttons[i].disabled = true;
    }
  }

  addUserClicked(user) {
    /*console.log('Add User ' + user.name + ' with role ' + user.role);*/
    this.data.name = user.name;
    this.data.role = user.role;
    if (this.data.role) {
      this.dialogRef.close(this.data);
    } else {
      alert('Please define a role for the user!');
    }
  }

}
