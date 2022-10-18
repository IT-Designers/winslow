import {Component, EventEmitter, OnInit, Input, Output} from '@angular/core';
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

  @Input() group = {name: '', members: []};

  @Output() memberEmitter = new EventEmitter();
  @Output() removeMember = new EventEmitter();

  allRoles = [''];
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

  filterFunction() {
    if (this.userSearchInput) {
      let i = 0;
      for (const member of this.group.members) {
        if (!member.name.includes(this.userSearchInput)) {
          this.group.members.splice(i, 1);
        }
        i++;
      }
    }
  }
  onRemoveItemClick(item) {
    const delIndex = this.group.members.findIndex((tempUser) => tempUser.name === item.name);
    this.group.members.splice(delIndex, 1);
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
          this.memberEmitter.emit(result);
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
