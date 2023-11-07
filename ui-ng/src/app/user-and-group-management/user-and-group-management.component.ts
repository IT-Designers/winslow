import {Component, OnInit} from '@angular/core';
import {GroupApiService, GroupInfo} from '../api/group-api.service';
import {RoleApiService} from '../api/role-api.service';
import {UserApiService} from '../api/user-api.service';
import {MatDialog} from '@angular/material/dialog';
import {DialogService} from '../dialog.service';
import {Link, UserInfo} from "../api/winslow-api";
import {AddUserComponent} from "./add-user-dialog/add-user.component";
import {NewGroupDialogComponent} from "./new-group-dialog/new-group-dialog.component";

@Component({
  selector: 'app-groups-view',
  templateUrl: './user-and-group-management.component.html',
  styleUrls: ['./user-and-group-management.component.css']
})
export class UserAndGroupManagementComponent implements OnInit {
  newGroup: GroupInfo = {name: '', members: []};
  itemSelected = false;
  myName = '';
  myUser: Link = {name: '', role: 'MEMBER'};

  allGroups: GroupInfo[] = [];
  allRoles = [''];

  userTabTooltip = '';
  allUsers: UserInfo[] = [];
  showUserDetail = false;
  selectedUser?: UserInfo;

  showGroupDetail = false;
  selectedGroup?: GroupInfo;

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

  groupClicked(group: GroupInfo) {
    this.selectedGroup = group;
    this.showGroupDetail = true;
  }

  userClicked(user: UserInfo) {
    this.selectedUser = user;
    this.showUserDetail = true;
  }

  onEditCancel() {
    this.selectedGroup = undefined;
    this.showGroupDetail = false;
    this.itemSelected = false;
  }

  onUserEditCancel() {
    this.selectedUser = undefined;
    this.showUserDetail = false;
  }

  onGroupDelete() {
    const group = this.selectedGroup;
    if (group == undefined) {
      this.dialog.error("Cannot delete group: No group selected.");
      return;
    }
    this.dialog.openAreYouSure(
      `Group being deleted: ${group.name}`,
      () => this.groupApi.deleteGroup(group.name)
        .then(() => {
          const delIndex = this.allGroups.findIndex((tempGroup) => tempGroup.name === group.name);
          this.allGroups.splice(delIndex, 1);
          this.allGroups = this.allGroups.concat([]);
          this.onEditCancel();
        })
    );
  }

  onUserDelete() {
    const user = this.selectedUser;
    if (user == undefined) {
      this.dialog.error("Cannot delete user: No user selected.");
      return;
    }
    this.dialog.openAreYouSure(
      `User being deleted: ${user.name}`,
      () => this.userApi.deleteUser(user.name)
        .then(() => {
          const delIndex = this.allUsers.findIndex((tempUser) => tempUser.name === user.name);
          this.allUsers.splice(delIndex, 1);
          this.allUsers = this.allUsers.concat([]);
          this.onUserEditCancel();
        })
    );
  }

  openNewUserDialog(): void {
    this.createDialog.open(AddUserComponent, {
      data: {} as string
    })
      .afterClosed()
      .subscribe((name) => {
        const newUser: UserInfo = {
          name,
          displayName: undefined,
          email: undefined,
          active: true,
          password: undefined,
        };
        this.dialog.openLoadingIndicator(this.userApi.createUser(newUser)
            .then(() => {
              this.allUsers.push(newUser);
              this.allUsers = this.allUsers.concat([]);
              this.selectedUser = newUser;
              this.showUserDetail = true;
            }),
          'Creating User');
        // TODO: actually create user, show progress with LoadingIndicator
      });
  }

  openNewGroupDialog(): void {
    this.createDialog.open(NewGroupDialogComponent, {
      data: {} as string
    })
      .afterClosed()
      .subscribe((name) => {
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
      });
  }
}
