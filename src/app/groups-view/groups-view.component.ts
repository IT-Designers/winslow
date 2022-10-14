import { Component, OnInit } from '@angular/core';
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
  showAddGroup = false;
  newGroup = {name: '', members: []};
  itemSelected = false;
  myName = '';
  myUser = [];

  allGroups = [];
  allRoles = [''];

  showGroupDetail = false;
  selectedGroup = {name: 'No Group Selected', members: []};

  constructor(
    private groupApi: GroupApiService,
    private roleApi: RoleApiService,
    private userApi: UserApiService,
    private createDialog: MatDialog,
    private dialog: DialogService) {
      this.groupApi.getGroups().then((groups) => this.allGroups = groups);
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
  onAddGroupToggle() {
    this.removeHighlighting();
    this.createDialog
      .open(GroupAddNameDialogComponent, {
        data: {
        } as string
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
              /* TODO: currently highlights previous last list entry */
              this.allGroups.push(newGroup);
              this.selectedGroup = newGroup;
              this.showGroupDetail = true;
              this.showAddGroup = false;
              this.removeHighlighting();
              const groupDivs = document.getElementsByClassName('group-list-item');
              groupDivs[groupDivs.length - 1].classList.add('item-clicked');
              this.itemSelected = true;
            }),
            'Creating Group');
        }
      });
  }
  groupClicked(group, event) {
    this.selectedGroup = group;
    this.showGroupDetail = true;
    this.removeHighlighting();
    event.target.classList.add('item-clicked');
    // event.target.parent.add('item-clicked');
    this.itemSelected = true;
  }
  onMemberAdded(event) {
    return this.dialog.openLoadingIndicator(
      this.groupApi.addOrUpdateMembership(this.selectedGroup.name, event).then(() => this.selectedGroup.members.push(event)),
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
    this.removeHighlighting();
    this.itemSelected = false;
  }
  removeHighlighting() {
    if (this.itemSelected) {
      const items = document.getElementsByClassName('group-list-item');
      const items2 = document.getElementsByClassName('item-icon');
      let i;
      for (i = 0; i < items.length; i++) {
        items[i].classList.remove('item-clicked');
      }
      for (i = 0; i < items2.length; i++) {
        items2[i].classList.remove('item-clicked');
      }
    }
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
              this.onEditCancel();
            }),
              'Deleting Group');
        }
      });
  }
}
