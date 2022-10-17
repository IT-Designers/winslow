import { Component, EventEmitter, OnInit, Input, Output } from '@angular/core';
import {RoleApiService} from '../api/role-api.service';
import {MatDialog} from '@angular/material/dialog';
import {DialogService} from '../dialog.service';
import {AddMemberData, GroupsAddMemberDialogComponent} from '../groups-add-member-dialog/groups-add-member-dialog.component';
import {GroupApiService} from '../api/group-api.service';

@Component({
  selector: 'app-group-member-list',
  templateUrl: './group-member-list.component.html',
  styleUrls: ['./group-member-list.component.css']
})
export class GroupMemberListComponent implements OnInit {

  @Input() items = [];    // left dropdown
  @Input() label = 'No Label:';     // text for label
  @Input() searchIdentifier = '';     // needs to be different for every dropdown component
  @Input() groupName = 'No Group';

  @Output() memberEmitter = new EventEmitter();
  @Output() removeMember = new EventEmitter();

  allRoles = [''];
  selected = '';
  userSearchInput = '';

  constructor(
    private roleApi: RoleApiService,
    private groupApi: GroupApiService,
    private createDialog: MatDialog,
    private dialog: DialogService) {
    this.roleApi.getRoles().then((roles) => this.allRoles = roles);
  }

  ngOnInit(): void {
  }
  filterFunction(inputId: string, divName: string) {
    let filter;
    let divs;
    let i;
    filter = this.userSearchInput.toUpperCase();
    divs = document.getElementsByClassName(divName);
    for (i = 0; i < divs.length; i++) {
      const txtValue = this.items[i].name;
      if (txtValue.toUpperCase().indexOf(filter) > -1) {
        divs[i].style.display = '';
      } else {
        divs[i].style.display = 'none';
      }
    }
  }
  onRemoveItemClick(item) {
    const delIndex = this.items.findIndex((tempUser) => tempUser.name === item.name);
    this.items.splice(delIndex, 1);
    this.removeMember.emit(item);
  }
  openAddMemberDialog() {
    this.createDialog
      .open(GroupsAddMemberDialogComponent, {
        data: {
        } as AddMemberData
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          /*return this.dialog.openLoadingIndicator(this.memberEmitter.emit(result), 'Adding Member to group');*/
          /*this.items.push(result);*/
          this.memberEmitter.emit(result);
        }
      });
  }
  roleChanged(user) {
    return this.dialog.openLoadingIndicator(
      this.groupApi.addOrUpdateMembership(this.groupName, user),
      'Adding Member to group'
    );
  }
  /*onAddItemClick(item) {
    const addIndex = this.items2.findIndex((tempUser) => tempUser.id === item.id);
    this.items.push(this.items2[addIndex]);
    this.items2.splice(addIndex, 1);
    this.items.sort((a, b) => a.id - b.id);
  }*/
  /*onOpenSecondDropdownClick() {
    this.showDropdown2 = !this.showDropdown2;
  }*/
}
