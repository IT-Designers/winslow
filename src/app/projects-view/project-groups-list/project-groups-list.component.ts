import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';
import {Group, ProjectApiService, ProjectInfo} from '../../api/project-api.service';
import {MatDialog} from '@angular/material/dialog';
import {DialogService} from '../../dialog.service';
import {AddGroupData, ProjectAddGroupDialogComponent} from '../project-add-group-dialog/project-add-group-dialog.component';

@Component({
  selector: 'app-project-groups-list',
  templateUrl: './project-groups-list.component.html',
  styleUrls: ['./project-groups-list.component.css']
})
export class ProjectGroupsListComponent implements OnInit, OnChanges {

  @Input() project: ProjectInfo;

  roles = ['OWNER', 'MEMBER'];
  groupSearchInput = '';
  displayGroups: Group[];


  constructor(
    private projectApi: ProjectApiService,
    private createDialog: MatDialog,
    private dialog: DialogService
  ) { }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges) {
    this.displayGroups = Array.from(this.project.groups);
  }

  filterFunction() {
    this.displayGroups = Array.from(this.project.groups);
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
  openAddGroupDialog() {
    this.createDialog
      .open(ProjectAddGroupDialogComponent, {
        data: {
          alreadyAssigned: this.displayGroups
        } as AddGroupData
      })
      .afterClosed()
      .subscribe((data) => {
        if (data) {
          const groupToAdd = {
            name: data.groupName,
            role: data.groupRole
          };
          this.dialog.openLoadingIndicator(
            this.projectApi.addOrUpdateGroup(this.project.id, groupToAdd),
            'Assigning Group to Project'
          );
          /*this.projectApi.addOrUpdateGroup(this.project.id, groupToAdd);*/
          this.displayGroups.push(groupToAdd);
        }
      });
  }
  onRemoveItemClick(item) {
    const delIndex = this.project.groups.findIndex((group) => group.name === item.name);
    this.project.groups.splice(delIndex, 1);
    const delIndex2 = this.displayGroups.findIndex((group) => group.name === item.name);
    this.displayGroups.splice(delIndex2, 1);
    return this.dialog.openLoadingIndicator(
      this.projectApi.removeGroup(this.project.id, item.name),
      'Removing Group from Project'
    );
  }
  roleChanged(group) {
    this.dialog.openLoadingIndicator(
      this.projectApi.addOrUpdateGroup(this.project.id, group),
      'Changing Group Role'
    );
  }
}
