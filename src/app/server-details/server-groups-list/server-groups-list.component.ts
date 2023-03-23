import {Component, Input, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {AddGroupData, ProjectAddGroupDialogComponent} from '../../project-view/project-add-group-dialog/project-add-group-dialog.component';
import {ErrorStateMatcher} from '@angular/material/core';
import {FormControl, FormGroupDirective, NgForm, Validators} from '@angular/forms';
import {NodeInfoExt, NodeResourceInfo, NodesApiService} from '../../api/nodes-api.service';

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
  @Input() node: NodeInfoExt;

  nodeResourceAllocations: NodeResourceInfo;

  maxMemory = 0;
  maxCpuCores = 0;
  maxGpus = 0;


  mockCpuCores = 16;
  mockCpuCoresFFA = 16;
  matcher = new MyErrorStateMatcher();
  cpuFormControl = new FormControl('', [
    Validators.max(this.maxCpuCores),
  ]);
  cpuFormControlFFA = new FormControl('', [
    Validators.max(this.mockCpuCoresFFA),
  ]);
  assignedCpuCores = '0';
  assignedCpuCoresFFA = '0';
  mockGpus = 3;
  mockGpusFFA = 3;
  gpuFormControl = new FormControl('', [
    Validators.max(this.mockGpus),
  ]);
  gpuFormControlFFA = new FormControl('', [
    Validators.max(this.mockGpusFFA),
  ]);
  assignedGpus = '0';
  assignedGpusFFA = '0';
  mockMaxMemory = 32768;
  mockMaxMemoryFFA = 32768;
  memoryFormControl = new FormControl('', [
    Validators.max(this.mockMaxMemory),
  ]);
  memoryFormControlFFA = new FormControl('', [
    Validators.max(this.mockMaxMemoryFFA),
  ]);
  assignedMemoryString = '0';
  assignedMemoryStringFFA = '0';
  assignedMemoryNumber = 0;
  assignedMemoryNumberFFA = 0;

  groupSearchInput = '';
  displayGroups: AssignedGroupInfo[] = null;
  roles = ['OWNER', 'MEMBER'];

  isServerFFA = false;

  testResourceObject = {
    freeForAll: false,
    globalLimit: {
      cpu: 0,
      gpu: 0,
      mem: 0
    },
    groupLimits: [
      {
        name: 'Group 1',
        resourceLimitation: {
          cpu: 8,
          gpu: 0,
          mem: 4096,
        },
        role: 'OWNER'
      },
      {
        name: 'Group 2',
        resourceLimitation: {
          cpu: 4,
          gpu: 1,
          mem: 2048,
        },
        role: 'MEMBER'
      }
    ]
  };

  constructor(private createDialog: MatDialog, private nodeApi: NodesApiService) {
  }

  ngOnInit(): void {
    this.maxMemory = (this.node.memInfo.memoryTotal / 1024 / 1024);
    this.maxGpus = this.node.gpuInfo.length;
    console.dir(this.node);
    this.nodeApi.setNodeResourceUsageConfiguration(this.testResourceObject, this.node.name)
      .then((result) => console.dir(result));
    this.nodeApi.getNodeResourceUsageConfiguration(this.node.name)
      .then((result) => {
        this.nodeResourceAllocations = result;
        console.dir(result);
      });
    this.displayGroups = Array.from(this.assignedGroups);
    this.cpuFormControl.setValue(this.assignedCpuCores);
    this.gpuFormControl.setValue(this.assignedGpus);
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


  cpuHasChanged(event, group) {
    group.resourceLimitation.cpu = event.value;
  }
  cpuHasChangedFFA(event) {
    this.nodeResourceAllocations.globalLimit.cpu = event.value;
  }

  gpuHasChanged(event, group) {
    group.resourceLimitation.gpu = event.value;
  }
  gpuHasChangedFFA(event) {
    this.nodeResourceAllocations.globalLimit.gpu = event.value;
  }

  memorySliderHasChanged(event, group) {
    group.resourceLimitation.mem = event.value;
  }
  getMemoryString(mem) {
    if (mem >= 1024) {
      return (mem / 1024).toFixed(2) + ' GiB';
    } else {
      return mem + ' MiB';
    }
  }
  memorySliderHasChangedFFA(event) {
    if (event.value >= 1024) {
      this.assignedMemoryStringFFA = (event.value / 1024).toFixed(2) + ' GiB';
    } else {
      this.assignedMemoryStringFFA = event.value + ' MiB';
    }
    this.assignedMemoryNumberFFA = event.value;
  }
  memoryInputHasChanged(event) {
    if (event.target.value >= 1024) {
      this.assignedMemoryString = (event.target.value / 1024).toFixed(2) + ' GiB';
    } else {
      this.assignedMemoryString = event.target.value + ' MiB';
    }
    this.assignedMemoryNumber = event.target.value;
  }
  memoryInputHasChangedFFA(event) {
    if (event.target.value >= 1024) {
      this.assignedMemoryStringFFA = (event.target.value / 1024).toFixed(2) + ' GiB';
    } else {
      this.assignedMemoryStringFFA = event.target.value + ' MiB';
    }
    this.assignedMemoryNumberFFA = event.target.value;
  }


}
