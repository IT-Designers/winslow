import {Component, OnInit} from '@angular/core';
import {GroupApiService, GroupInfo} from '../api/group-api.service';
import {RoleApiService} from '../api/role-api.service';
import {UserApiService, UserInfo} from '../api/user-api.service';
import {MatLegacyDialog as MatDialog} from '@angular/material/legacy-dialog';
import {DialogService} from '../dialog.service';

@Component({
  selector: 'app-groups-view',
  templateUrl: './user-and-group-management.component.html',
  styleUrls: ['./user-and-group-management.component.css']
})
export class UserAndGroupManagementComponent implements OnInit {
  newGroup = {name: '', members: []};
  itemSelected = false;
  myName = '';
  myUser = {name: '', role: ''};

  allGroups = [];
  allRoles = [''];

  userTabTooltip = '';
  allUsers = [];
  showUserDetail = false;
  selectedUser: UserInfo = null;

  showGroupDetail = false;
  selectedGroup: GroupInfo = null;

  constructor(
    private groupApi: GroupApiService,
    private roleApi: RoleApiService,
    private userApi: UserApiService,
    private createDialog: MatDialog,
    private dialog: DialogService) {
    this.groupApi.getGroups().then((groups) => {
      this.allGroups = groups;
    });
    this.userApi.getUsers().then((users) => {
      this.allUsers = Array.from(users);
      /*this.displayUsers = Array.from(users);*/
      /*this.sortDisplayUsersByName();*/
    });
    this.roleApi.getRoles().then((roles) => this.allRoles = roles);
    this.userApi.getSelfUserName().then((name) => {
      this.myName = name;
      this.myUser = {name: this.myName, role: 'OWNER'};
      if (this.newGroup.members.length === 0) {
        this.newGroup.members.push(this.myUser);
      }
    });
  }

  ngOnInit(): void {
  }

  isUserAdmin() {
    // TODO: Check if current User is admin
    // TODO: Set userTabTooltip according to user admin status
    return true;
  }

  /*sortDisplayUsersByName() {
    this.displayUsers.sort((a, b) => {
      if (a.name.toUpperCase() > b.name.toUpperCase()) {
        return 1;
      } else {
        return -1;
      }
    });
  }*/

  onAddGroupToggle(name) {
    if (name) {
      const newGroup = {
        name,
        members: [this.myUser],
      };
      return this.dialog.openLoadingIndicator(this.groupApi.createGroup(newGroup)
          .then(() => {
            this.allGroups.push(newGroup);
            this.allGroups = this.allGroups.concat([]);
            this.selectedGroup = newGroup;
            this.showGroupDetail = true;
          }),
        'Creating Group');
    }
  }

  onAddUserToggle(name) {
    if (name) {
      const newUser = {
        name,
        displayName: null,
        email: null,
        active: true,
        password: null,
      };
      return this.dialog.openLoadingIndicator(this.userApi.createUser(newUser)
          .then(() => {
            this.allUsers.push(newUser);
            this.allUsers = this.allUsers.concat([]);
            this.selectedUser = newUser;
            this.showUserDetail = true;
          }),
        'Creating User');
      // TODO: actually create user, show progress with LoadingIndicator
    }
  }

  groupClicked(group) {
    this.selectedGroup = group;
    this.showGroupDetail = true;
  }

  userClicked(user) {
    this.selectedUser = user;
    this.showUserDetail = true;
  }

  onEditCancel() {
    this.selectedGroup = null;
    this.showGroupDetail = false;
    this.itemSelected = false;
  }

  onUserEditCancel() {
    this.selectedUser = null;
    this.showUserDetail = false;
  }

  onGroupDelete() {
    this.dialog.openAreYouSure(
      `Group being deleted: ${this.selectedGroup.name}`,
      () => this.groupApi.deleteGroup(this.selectedGroup.name)
        .then(() => {
          const delIndex = this.allGroups.findIndex((tempGroup) => tempGroup.name === this.selectedGroup.name);
          this.allGroups.splice(delIndex, 1);
          this.allGroups = this.allGroups.concat([]);
          this.onEditCancel();
        })
    );
  }

  onUserDelete() {
    this.dialog.openAreYouSure(
      `User being deleted: ${this.selectedUser.name}`,
      () => this.userApi.deleteUser(this.selectedUser.name)
        .then(() => {
          const delIndex = this.allUsers.findIndex((tempUser) => tempUser.name === this.selectedUser.name);
          this.allUsers.splice(delIndex, 1);
          this.allUsers = this.allUsers.concat([]);
          this.onUserEditCancel();
        })
    );
  }
}
