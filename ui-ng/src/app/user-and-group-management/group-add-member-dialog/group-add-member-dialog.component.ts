import {Component, Inject, OnInit} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import {UserApiService} from '../../api/user-api.service';
import {RoleApiService} from '../../api/role-api.service';
import {Role, UserInfo} from '../../api/winslow-api';

export interface AddMemberData {
  members: UserInfo[];
  name: string;
  role: string;
}

@Component({
  selector: 'app-groups-add-member-dialog',
  templateUrl: './group-add-member-dialog.component.html',
  styleUrls: ['./group-add-member-dialog.component.css']
})
export class GroupAddMemberDialogComponent implements OnInit {

  allUsers: UserInfo[] = [];
  displayUsers: UserInfo[] = [];
  allRoles: string[] = [];
  userSearchInput = '';
  showUsersToggle = false;
  selectedRole: Role = "MEMBER";

  constructor(
    public dialogRef: MatDialogRef<GroupAddMemberDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AddMemberData,
    private userApi: UserApiService,
    private roleApi: RoleApiService
  ) { }

  ngOnInit(): void {
    this.userApi.getUsers().then((users) => {
      this.allUsers = users.filter((user) => {
        for (const member of this.data.members) {
          if (member.name === user.name) {
            return false;
          }
        }
        return true;
      });
    });
    this.roleApi.getRoles().then((roles) => this.allRoles = roles);
  }

  sortUsers() {
    this.allUsers.sort((a, b) => {
      if (a.name.toUpperCase() > b.name.toUpperCase()) {
        return 1;
      } else {
        return -1;
      }
    });
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
    this.displayUsers = [];
    this.sortUsers();
    const searchedUsers = [];
    for (const user of this.allUsers) {
      if (this.userSearchInput) {
        if (user.name.toUpperCase().includes(this.userSearchInput.toUpperCase())) {
          searchedUsers.push(user);
        }
      } else {
        searchedUsers.push(user);
      }
    }
    this.displayUsers = Array.from(searchedUsers);
  }

  addUserClicked(name: string, role: Role) {
    this.data.name = name;
    this.data.role = role;
    if (this.data.role) {
      this.dialogRef.close(this.data);
    } else {
      alert('Please define a role for the user!');
    }
  }

}
