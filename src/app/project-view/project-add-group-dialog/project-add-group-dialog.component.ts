import {Component, Inject, OnInit} from '@angular/core';
import {GroupInfo} from '../../api/group-api.service';

import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {GroupApiService} from '../../api/group-api.service';

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
  allGroups: GroupInfo[];
  displayGroups: GroupInfo[];
  allRoles = ['OWNER', 'MEMBER'];

  constructor(
    public dialogRef: MatDialogRef<ProjectAddGroupDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AddGroupData,
    private groupApi: GroupApiService
  ) { }

  ngOnInit(): void {
    this.groupApi.getGroups().then((groups) => {
      this.allGroups = groups.filter((group) => {
        for (const allreadyAssignedGroup of this.data.alreadyAssigned) {
          if (allreadyAssignedGroup.name === group.name) {
            return false;
          }
        }
        return true;
      });
    });
  }

  sortGroups() {
    this.displayGroups.sort((a, b) => {
      if (a.name.toUpperCase() > b.name.toUpperCase()) {
        return 1;
      } else {
        return -1;
      }
    });
  }

  filterFunction() {
    this.displayGroups = Array.from(this.allGroups);
    this.sortGroups();
    if (this.groupSearchInput) {
      const searchedGroups = [];
      for (const user of this.displayGroups) {
        if (user.name.toUpperCase().includes(this.groupSearchInput.toUpperCase())) {
          searchedGroups.push(user);
        }
      }
      this.displayGroups = Array.from(searchedGroups);
    }
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

  addGroupClicked(group) {
    this.data.groupName = group.name;
    this.data.groupRole = group.role;
    if (this.data.groupRole) {
      this.dialogRef.close(this.data);
    } else {
      alert('Please define a role for the user!');
    }
  }

}
