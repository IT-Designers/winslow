import {Component, OnInit} from '@angular/core';
import { GroupApiService } from '../api/group-api.service';
import { RoleApiService } from '../api/role-api.service';
import {UserApiService} from '../api/user-api.service';
import {MatDialog} from '@angular/material/dialog';
import {DialogService} from '../dialog.service';
import {GroupAddNameDialogComponent} from '../group-add-name-dialog/group-add-name-dialog.component';
import {UserAddNameDialogComponent} from './user-add-name-dialog/user-add-name-dialog.component';

@Component({
  selector: 'app-groups-view',
  templateUrl: './groups-view.component.html',
  styleUrls: ['./groups-view.component.css']
})
export class GroupsViewComponent implements OnInit {
  newGroup = {name: '', members: []};
  itemSelected = false;
  myName = '';
  myUser = {name: '', role: ''};

  allGroups = [];
  displayGroups = [];
  allRoles = [''];
  showSystemGroups = false;
  groupSearchInput = '';

  userTabTooltip = '';
  allUsers = [];
  displayUsers = [];
  showUserDetail = false;
  selectedUser = {name: 'No User Selected'};

  showGroupDetail = false;
  selectedGroup = {name: 'No Group Selected', members: []};

  constructor(
    private groupApi: GroupApiService,
    private roleApi: RoleApiService,
    private userApi: UserApiService,
    private createDialog: MatDialog,
    private dialog: DialogService) {
      this.groupApi.getGroups().then((groups) => {
        this.allGroups = groups;
        this.filterSystemGroups();
        this.searchGroupFilter();
      });
      this.userApi.getUsers().then((users) => {
        this.allUsers = Array.from(users);
        this.displayUsers = Array.from(users);
        this.sortDisplayUsersByName();
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
  sortDisplayGroupsByName() {
    this.displayGroups.sort((a, b) => {
      if (a.name.toUpperCase() > b.name.toUpperCase()) {
        return 1;
      } else {
        return -1;
      }
    });
  }
  sortDisplayUsersByName() {
    this.displayUsers.sort((a, b) => {
      if (a.name.toUpperCase() > b.name.toUpperCase()) {
        return 1;
      } else {
        return -1;
      }
    });
  }
  filterSystemGroups() {
    if (!this.showSystemGroups) {
      let i = 0;
      this.displayGroups = Array.from(this.allGroups);
      this.sortDisplayGroupsByName();
      for (const group of this.displayGroups) {
        if (group.name.includes('::')) {
          this.displayGroups.splice(i, 1);
          i--;
        }
        i++;
      }
    } else if (this.showSystemGroups) {
      this.displayGroups = Array.from(this.allGroups);
      this.sortDisplayGroupsByName();
    }
  }
  searchGroupFilter() {
    this.filterSystemGroups();

    let searchedGroups = Array.from(this.displayGroups);
    if (this.groupSearchInput !== '') {
      searchedGroups = [];
      for (const group of this.displayGroups) {
        if (group.name.toUpperCase().includes(this.groupSearchInput.toUpperCase())) {
          searchedGroups.push(group);
        }
      }
      this.displayGroups = Array.from(searchedGroups);
      this.sortDisplayGroupsByName();
    }
  }
  onAddGroupToggle() {
    this.createDialog
      .open(GroupAddNameDialogComponent, {
        data: {} as string
      })
      .afterClosed()
      .subscribe((name) => {
        if (name) {
          const newGroup = {
            members: [this.myUser],
            name,
          };
          return this.dialog.openLoadingIndicator(this.groupApi.createGroup(newGroup)
            .then(() => {
              this.allGroups.push(newGroup);
              this.displayGroups.push(newGroup);
              this.sortDisplayGroupsByName();
              this.selectedGroup = newGroup;
              this.showGroupDetail = true;
            }),
            'Creating Group');
        }
      });
  }
  onAddUserToggle() {
    console.log('Add New User pressed');
    this.createDialog
      .open(UserAddNameDialogComponent, {
        data: {} as string
      })
      .afterClosed()
      .subscribe((name) => {
        if (name) {
          const newUser = {
            name
          };
          console.log('Creating new user: ' + newUser.name);
          // TODO: actually create user, show progress with LoadingIndicator
        }
      });
  }
  groupClicked(group) {
    this.selectedGroup = group;
    this.showUserDetail = false;
    this.showGroupDetail = true;
  }
  userClicked(user) {
    this.selectedUser = user;
    this.showGroupDetail = false;
    this.showUserDetail = true;
  }
  onMemberAdded(event) {
    return this.dialog.openLoadingIndicator(
      this.groupApi.addOrUpdateMembership(this.selectedGroup.name, event)
        .then(() => {
          this.selectedGroup.members.push(event);
          // this.removeHighlighting();
          /*const groupDivs = document.getElementsByClassName('group-list-item');
          groupDivs[groupDivs.length - 1].classList.add('item-clicked');*/
        }),
      'Adding Member to group'
    );
  }
  onRemoveMember(event) {
    return this.dialog.openLoadingIndicator(
      this.groupApi.deleteGroupMembership(this.selectedGroup.name, event.name),
      'Removing Member from Group'
    );
  }
  onEditCancel() {
    this.selectedGroup = {name: 'No Group Selected', members: []};
    this.showGroupDetail = false;
    this.itemSelected = false;
  }
  onGroupDelete() {
    this.dialog.openAreYouSure(
      `Group being deleted: ${this.selectedGroup.name}`,
      () => this.groupApi.deleteGroup(this.selectedGroup.name)
            .then(() => {
              const delIndex = this.allGroups.findIndex((tempGroup) => tempGroup.name === this.selectedGroup.name);
              this.allGroups.splice(delIndex, 1);
              const delIndex2 = this.displayGroups.findIndex((tempGroup) => tempGroup.name === this.selectedGroup.name);
              this.displayGroups.splice(delIndex2, 1);
              this.onEditCancel();
            })
    );
  }
  onUserDelete() {
    console.log('User Delete Pressed');
    // TODO: Implement AreYouSure Dialog and actual API to delete
  }
}
