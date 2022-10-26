import {Component, EventEmitter, OnInit, OnChanges, Input, Output, SimpleChanges} from '@angular/core';
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
export class GroupMemberListComponent implements OnInit, OnChanges {

  @Input() group = {name: '', members: []};

  @Output() memberEmitter = new EventEmitter();
  @Output() removeMember = new EventEmitter();

  allRoles = [''];
  userSearchInput = '';
  displayMembers = [];
  disabledUser: {name: '', role: ''};

  constructor(
    private roleApi: RoleApiService,
    private groupApi: GroupApiService,
    private createDialog: MatDialog,
    private dialog: DialogService) {
    this.roleApi.getRoles().then((roles) => this.allRoles = roles);
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges) {
    this.displayMembers = Array.from(this.group.members);
    this.checkSelects();
  }

  filterFunction() {
    this.displayMembers = Array.from(this.group.members);
    if (this.userSearchInput !== '') {
      const searchedMembers = [];
      for (const member of this.displayMembers) {
        if (member.name.toUpperCase().includes(this.userSearchInput.toUpperCase())) {
          searchedMembers.push(member);
        }
      }
      this.displayMembers = Array.from(searchedMembers);
    }
  }
  onRemoveItemClick(item) {
    this.removeMember.emit(item);
    const delIndex = this.group.members.findIndex((tempUser) => tempUser.name === item.name);
    this.group.members.splice(delIndex, 1);
    const delIndex2 = this.displayMembers.findIndex((tempUser) => tempUser.name === item.name);
    this.displayMembers.splice(delIndex2, 1);
    this.checkSelects();
  }
  openAddMemberDialog() {
    this.createDialog
      .open(GroupsAddMemberDialogComponent, {
        data: {
          members: this.displayMembers
        } as AddMemberData
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.memberEmitter.emit(result);
          this.displayMembers.push(result);
          this.checkSelects();
        }
      });
  }
  roleChanged(user) {
    this.checkSelects();
    return this.dialog.openLoadingIndicator(
      this.groupApi.addOrUpdateMembership(this.group.name, user),
      'Adding Member to group'
    );
  }
  checkSelects() {
    if (this.displayMembers.length === 1) {
      this.disabledUser = this.displayMembers[0];
    } else {
      let ownerCount = 0;
      for (const member of this.displayMembers) {
        if (member.role === 'OWNER') {
          ownerCount++;
          this.disabledUser = member;
        }
      }
      if (ownerCount > 1) {
        this.disabledUser = {name: '', role: ''};
      }
    }
  }
}
