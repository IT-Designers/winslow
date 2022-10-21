import {Component, OnInit} from '@angular/core';
import { GroupApiService } from '../api/group-api.service';
import { RoleApiService } from '../api/role-api.service';
import {UserApiService} from '../api/user-api.service';
import {MatDialog} from '@angular/material/dialog';
import {DialogService} from '../dialog.service';
import {ResponseData, DeleteConfirmDialogComponent} from '../delete-confirm-dialog/delete-confirm-dialog.component';
import {GroupAddNameDialogComponent} from '../group-add-name-dialog/group-add-name-dialog.component';

@Component({
  selector: 'app-groups-view',
  templateUrl: './groups-view.component.html',
  styleUrls: ['./groups-view.component.css']
})
export class GroupsViewComponent implements OnInit {
  newGroup = {name: '', members: []};
  itemSelected = false;
  myName = '';
  myUser = [];

  allGroups = [];
  displayGroups = [];
  allRoles = [''];
  showSystemGroups = false;
  groupSearchInput = '';

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
      this.roleApi.getRoles().then((roles) => this.allRoles = roles);
      this.userApi.getSelfUserName().then((name) => {
      this.myName = name;
      this.myUser = [{name: this.myName, role: 'OWNER'}];
      if (this.newGroup.members.length === 0) {
        this.newGroup.members = this.myUser;
      }
      });
  }

  ngOnInit(): void {
  }

  filterSystemGroups() {
    if (!this.showSystemGroups) {
      let i = 0;
      this.displayGroups = Array.from(this.allGroups);
      for (const group of this.displayGroups) {
        if (group.name.includes('::')) {
          this.displayGroups.splice(i, 1);
        }
        i++;
      }
    } else if (this.showSystemGroups) {
      this.displayGroups = Array.from(this.allGroups);
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
    }
    /*if (this.groupSearchInput !== '') {
      const input = this.groupSearchInput;
      const filter = input.toUpperCase();
      for (let i = 0; i < this.displayGroups.length; i++) {
        const txtValue = this.displayGroups[i].name;
        if (!(txtValue.toUpperCase().indexOf(filter) > -1)) {
          this.displayGroups.splice(i, 1);
        }
      }
    }*/
      /*let i = 0;
      for (const group of this.displayGroups) {
        console.log('Now checking group ///' + group.name + '/// with input ///' + this.groupSearchInput + '///');
        console.dir(group.name);
        if (!group.name.includes(this.groupSearchInput)) {
          // console.log('Group number ' + i + ' with name ' + group.name + ' doesnt include string ' + this.groupSearchInput);
          console.log('--------------- Removing Group: ');
          console.dir(this.displayGroups[i]);
          this.displayGroups.splice(i, 1);
        }
        i++;
      }*/
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
            members: this.myUser,
            name,
          };
          return this.dialog.openLoadingIndicator(this.groupApi.createGroup(newGroup)
            .then(() => {
              this.allGroups.push(newGroup);
              this.displayGroups.push(newGroup);
              this.selectedGroup = newGroup;
              this.showGroupDetail = true;
            }),
            'Creating Group');
        }
      });
  }
  groupClicked(group) {
    this.selectedGroup = group;
    this.showGroupDetail = true;
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
    this.createDialog
      .open(DeleteConfirmDialogComponent, {
        data: {
        } as ResponseData
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          return this.dialog.openLoadingIndicator(this.groupApi.deleteGroup(this.selectedGroup.name)
            .then(() => {
              const delIndex = this.allGroups.findIndex((tempGroup) => tempGroup.name === this.selectedGroup.name);
              this.allGroups.splice(delIndex, 1);
              const delIndex2 = this.displayGroups.findIndex((tempGroup) => tempGroup.name === this.selectedGroup.name);
              this.displayGroups.splice(delIndex2, 1);
              this.onEditCancel();
            }),
              'Deleting Group');
        }
      });
  }
}
