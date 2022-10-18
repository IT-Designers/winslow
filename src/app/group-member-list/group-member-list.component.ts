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
  }

  filterFunction() {
    this.displayMembers = Array.from(this.group.members);
    if (this.userSearchInput) {
      let i = 0;
      for (const member of this.displayMembers) {
        if (!member.name.includes(this.userSearchInput)) {
          this.displayMembers.splice(i, 1);
        }
        i++;
      }
    }
  }
  onRemoveItemClick(item) {
    this.removeMember.emit(item);
    const delIndex = this.group.members.findIndex((tempUser) => tempUser.name === item.name);
    this.group.members.splice(delIndex, 1);
    const delIndex2 = this.displayMembers.findIndex((tempUser) => tempUser.name === item.name);
    this.displayMembers.splice(delIndex2, 1);
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
          this.memberEmitter.emit(result);
          this.displayMembers.push(result);
        }
      });
  }
  roleChanged(user) {
    return this.dialog.openLoadingIndicator(
      this.groupApi.addOrUpdateMembership(this.group.name, user),
      'Adding Member to group'
    );
  }
}
