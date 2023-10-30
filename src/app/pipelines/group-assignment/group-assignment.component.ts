import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {AddGroupData, ProjectAddGroupDialogComponent} from '../../project-view/project-add-group-dialog/project-add-group-dialog.component';
import {MatDialog} from '@angular/material/dialog';
import {Link} from "../../api/winslow-api";

@Component({
  selector: 'app-group-assignment',
  templateUrl: './group-assignment.component.html',
  styleUrls: ['./group-assignment.component.css']
})
export class GroupAssignmentComponent implements OnInit {

  @Input() currentlyAssignedGroups!: Link[];

  @Output() groupAssignmentRemovedEmitter: EventEmitter<Link> = new EventEmitter();
  @Output() groupAddedEmitter: EventEmitter<Link> = new EventEmitter();

  showGroupList = false;
  groupListBtnText = 'Expand';
  groupListBtnIcon = 'expand_more';

  groupSearchInput = '';
  displayGroups!: Link[];
  roles = ['OWNER', 'MEMBER'];

  constructor(private createDialog: MatDialog) { }

  ngOnInit(): void {
    this.displayGroups = [...this.currentlyAssignedGroups];
  }

  remove(group: Link) {
    this.groupAssignmentRemovedEmitter.emit(group);
  }

  onRemoveItemClick(item: Link) {
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
  roleChanged(group: Link) {
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

  getChipColor(group: Link) {
    if (group.role === 'OWNER') {
      return '#8ed69b';
    } else {
      return '#d88bca';
    }
  }

  getTooltip(group: Link): string {
    if (group.role === 'OWNER') {
      return 'OWNER';
    } else if (group.role === 'MEMBER') {
      return 'MEMBER';
    }
    return '';
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
