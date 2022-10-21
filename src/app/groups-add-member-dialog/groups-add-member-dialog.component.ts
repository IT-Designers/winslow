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
    }
    if (this.showUsersToggle) {
      this.filterFunction();
    }
  }

  filterFunction() {
    this.displayUsers = Array.from(this.allUsers);
    if (this.userSearchInput) {
      const searchedUsers = [];
      for (const user of this.displayUsers) {
        if (user.name.toUpperCase().includes(this.userSearchInput.toUpperCase())) {
          searchedUsers.push(user);
        }
      }
      this.displayUsers = Array.from(searchedUsers);
    }
    /*if (this.userSearchInput) {
      let i = 0;
      for (const user of this.displayUsers) {
        if (!user.name.includes(this.userSearchInput)) {
          this.displayUsers.splice(i, 1);
        }
        i++;
      }
    }*/
  }

  addUserClicked(user) {
    this.data.name = user.name;
    this.data.role = user.role;
    if (this.data.role) {
      this.dialogRef.close(this.data);
    } else {
      alert('Please define a role for the user!');
    }
  }

}
