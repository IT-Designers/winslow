import {Component, Input, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {AddGroupData, ProjectAddGroupDialogComponent} from '../../project-view/project-add-group-dialog/project-add-group-dialog.component';
import {ErrorStateMatcher} from '@angular/material/core';
import {FormControl, FormGroupDirective, NgForm, Validators} from '@angular/forms';

export interface AssignedGroupInfo {
  name: string;
  role: string;
}

export class MyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: FormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const isSubmitted = form && form.submitted;
    return !!(control && control.invalid && (control.dirty || control.touched || isSubmitted));
  }
}

@Component({
  selector: 'app-server-groups-list',
  templateUrl: './server-groups-list.component.html',
  styleUrls: ['./server-groups-list.component.css']
})
export class ServerGroupsListComponent implements OnInit {

  @Input() assignedGroups: AssignedGroupInfo[] = null;

  mockCpuCores = 16;
  matcher = new MyErrorStateMatcher();
  cpuFormControl = new FormControl('', [
    Validators.max(this.mockCpuCores),
  ]);
  assignedCpuCores = '0';
  mockGpus = 3;
  gpuFormControl = new FormControl('', [
    Validators.max(this.mockGpus),
  ]);
  assignedGpus = '0';
  mockMaxMemory = 32768;
  memoryFormControl = new FormControl('', [
    Validators.max(this.mockMaxMemory),
  ]);
  assignedMemory = '0';
  assignedMemoryNumber = 0;

  groupSearchInput = '';
  displayGroups: AssignedGroupInfo[] = null;
  roles = ['OWNER', 'MEMBER'];

  isServerFFA = false;

  constructor(private createDialog: MatDialog) {
  }

  ngOnInit(): void {
    this.displayGroups = Array.from(this.assignedGroups);
  }

  filterFunction() {
    this.displayGroups = Array.from(this.assignedGroups);
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
        } as unknown as AddGroupData
      })
      .afterClosed()
      .subscribe((data) => {
        if (data) {
          const groupToAdd = {
            name: data.groupName,
            role: data.groupRole
          };
          /*this.dialog.openLoadingIndicator(
            this.projectApi.addOrUpdateGroup(this.project.id, groupToAdd),
            'Assigning Group to Project'
          );*/
          /*this.projectApi.addOrUpdateGroup(this.project.id, groupToAdd);*/
          console.dir(groupToAdd);
          this.displayGroups.push(groupToAdd);
          // this.newGroupEmitter.emit(groupToAdd);
        }
      });
  }

  onRemoveItemClick(item) {
    const delIndex = this.assignedGroups.findIndex((group) => group.name === item.name);
    this.assignedGroups.splice(delIndex, 1);
    const delIndex2 = this.displayGroups.findIndex((group) => group.name === item.name);
    this.displayGroups.splice(delIndex2, 1);
    console.log('Remove ' + item.name + ' from list');
    /*return this.dialog.openLoadingIndicator(
      this.projectApi.removeGroup(this.project.id, item.name),
      'Removing Group from Project'
    );*/
  }

  roleChanged(group) {
    console.log('Role changed to ' + group.role);
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


  cpuHasChanged(event) {
    this.assignedCpuCores = event.value;
  }

  gpuHasChanged(event) {
    this.assignedGpus = event.value;
  }

  memorySliderHasChanged(event) {
    if (event.value >= 1024) {
      this.assignedMemory = (event.value / 1024).toFixed(2) + ' GiB';
    } else {
      this.assignedMemory = event.value + ' MiB';
    }
    this.assignedMemoryNumber = event.value;
  }
  memoryInputHasChanged(event) {
    if (event.target.value >= 1024) {
      this.assignedMemory = (event.target.value / 1024).toFixed(2) + ' GiB';
    } else {
      this.assignedMemory = event.target.value + ' MiB';
    }
    this.assignedMemoryNumber = event.target.value;
  }

}
