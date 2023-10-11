import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {AddGroupData, ProjectAddGroupDialogComponent} from '../../project-view/project-add-group-dialog/project-add-group-dialog.component';
import {MatDialog} from '@angular/material/dialog';

export interface AssignedGroupInfo {
  name: string;
  role: string;
}

@Component({
  selector: 'app-group-assignment',
  templateUrl: './group-assignment.component.html',
  styleUrls: ['./group-assignment.component.css']
})
export class GroupAssignmentComponent implements OnInit {

  @Input() currentlyAssignedGroups: AssignedGroupInfo[] = null;

  @Output() groupAssignmentRemovedEmitter = new EventEmitter();
  @Output() groupAddedEmitter = new EventEmitter();

  showGroupList = false;
  groupListBtnText = 'Expand';
  groupListBtnIcon = 'expand_more';

  groupSearchInput = '';
  displayGroups: AssignedGroupInfo[] = null;
  roles = ['OWNER', 'MEMBER'];

  constructor(private createDialog: MatDialog) { }

  ngOnInit(): void {
    this.displayGroups = Array.from(this.currentlyAssignedGroups);
  }

  remove(group) {
    this.groupAssignmentRemovedEmitter.emit(group);
  }

  onRemoveItemClick(item) {
    this.groupAssignmentRemovedEmitter.emit(item);
    const delIndex = this.currentlyAssignedGroups.findIndex((group) => group.name === item.name);
    this.currentlyAssignedGroups.splice(delIndex, 1);
    const delIndex2 = this.displayGroups.findIndex((group) => group.name === item.name);
    this.displayGroups.splice(delIndex2, 1);
    console.log('Remove ' + item.name + ' from list');
  }
  openAddGroupDialog() {
    this.createDialog
      .open(ProjectAddGroupDialogComponent, {
        data: {
          alreadyAssigned: this.displayGroups
        } as unknown as AddGroupData
      })
      .afterClosed()
      .subscribe((data) => {
        if (data) {
          const groupToAdd = {
            name: data.groupName,
            role: data.groupRole
          };

          this.displayGroups.push(groupToAdd);
          this.groupAddedEmitter.emit(groupToAdd);
        }
      });
  }
  roleChanged(group) {
    console.log('Role changed to ' + group.role);
  }
  filterFunction() {
    this.displayGroups = Array.from(this.currentlyAssignedGroups);
    if (this.groupSearchInput !== '') {
      const searchedMembers = [];
      for (const member of this.displayGroups) {
        if (member.name.toUpperCase().includes(this.groupSearchInput.toUpperCase())) {
          searchedMembers.push(member);
        }
      }
      this.displayGroups = Array.from(searchedMembers);
    }
  }

  getChipColor(group) {
    if (group.role === 'OWNER') {
      return '#8ed69b';
    } else {
      return '#d88bca';
    }
  }

  getTooltip(group) {
    if (group.role === 'OWNER') {
      return 'OWNER';
    } else if (group.role === 'MEMBER') {
      return 'MEMBER';
    }
  }

  changeGroupListBtnTextAndIcon() {
    this.showGroupList = !this.showGroupList;
    if (this.groupListBtnText === 'Expand') {
      this.groupListBtnText = 'Collapse';
      this.groupListBtnIcon = 'expand_less';
    } else if (this.groupListBtnText === 'Collapse') {
      this.groupListBtnText = 'Expand';
      this.groupListBtnIcon = 'expand_more';
    }
  }
}
