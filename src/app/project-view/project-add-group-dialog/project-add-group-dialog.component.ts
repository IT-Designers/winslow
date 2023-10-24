import {Component, Inject, OnInit} from '@angular/core';
import {GroupInfo} from '../../api/group-api.service';

import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {GroupApiService} from '../../api/group-api.service';
import {Link, Role} from '../../api/winslow-api';

export interface AddGroupData {
  alreadyAssigned: GroupInfo[];
  groupName: string;
  groupRole: string;
}

@Component({
  selector: 'app-project-add-group-dialog',
  templateUrl: './project-add-group-dialog.component.html',
  styleUrls: ['./project-add-group-dialog.component.css']
})
export class ProjectAddGroupDialogComponent implements OnInit {

  groupSearchInput = '';
  showGroupsToggle = false;
  allGroups: GroupInfo[] = [];
  displayGroups: GroupInfo[] = [];
  allRoles = ['OWNER', 'MEMBER'];
  selectedRole: Role = "MEMBER";

  constructor(
    public dialogRef: MatDialogRef<ProjectAddGroupDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AddGroupData,
    private groupApi: GroupApiService
  ) { }

  ngOnInit(): void {
    this.groupApi.getGroups().then((groups) => {
      this.allGroups = groups.filter((group) => {
        for (const alreadyAssignedGroup of this.data.alreadyAssigned) {
          if (alreadyAssignedGroup.name === group.name) {
            return false;
          }
        }
        return true;
      });
    });
  }

  sortGroups() {
    this.allGroups.sort((a, b) => {
      if (a.name.toUpperCase() > b.name.toUpperCase()) {
        return 1;
      } else {
        return -1;
      }
    });
  }

  filterFunction() {
    this.displayGroups = [];
    this.sortGroups();
    const searchedGroups = [];
    for (const group of this.allGroups) {
      if (this.groupSearchInput) {
        if (group.name.toUpperCase().includes(this.groupSearchInput.toUpperCase())) {
          searchedGroups.push(group);
        }
      } else {
        searchedGroups.push(group);
      }
    }
    this.displayGroups = Array.from(searchedGroups);
  }

  onKeyUp() {
    if (this.groupSearchInput.length >= 2) {
      this.showGroupsToggle = true;
      this.filterFunction();
    }
    if (this.showGroupsToggle) {
      this.filterFunction();
    }
  }

  addGroupClicked(name: string, role: Role) {
    this.data.groupName = name;
    this.data.groupRole = role;
    if (this.data.groupRole) {
      this.dialogRef.close(this.data);
    } else {
      alert('Please define a role for the user!');
    }
  }

}
