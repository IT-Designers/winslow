import {Component, Input, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {AddGroupData, ProjectAddGroupDialogComponent} from '../../project-view/project-add-group-dialog/project-add-group-dialog.component';
import {ErrorStateMatcher} from '@angular/material/core';
import {FormControl, FormGroupDirective, NgForm, Validators} from '@angular/forms';
import {NodeInfoExt, NodeResourceInfo, NodesApiService} from '../../api/nodes-api.service';
import {DialogService} from '../../dialog.service';

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

  defaultResourceObject = {
    freeForAll: false,
    globalLimit: {
      cpu: 0,
      gpu: 0,
      mem: 0
    },
    groupLimits: []
  };
  nodeResourceAllocations: NodeResourceInfo = this.defaultResourceObject;
  editableResourceAllocations: NodeResourceInfo = this.defaultResourceObject;

  maxMemory = 0;
  maxCpuCores = 0;
  maxGpus = 0;

  matcher = new MyErrorStateMatcher();
  cpuFormControl = new FormControl('', [
    Validators.max(this.maxCpuCores),
  ]);
  cpuFormControlFFA = new FormControl('', [
    Validators.max(this.maxCpuCores),
  ]);

  gpuFormControl = new FormControl('', [
    Validators.max(this.maxGpus),
  ]);
  gpuFormControlFFA = new FormControl('', [
    Validators.max(this.maxGpus),
  ]);
  memoryFormControl = new FormControl('', [
    Validators.max(this.maxMemory),
  ]);
  memoryFormControlFFA = new FormControl('', [
    Validators.max(this.maxMemory),
  ]);

  groupSearchInput = '';
  displayGroups: AssignedGroupInfo[] = null;
  roles = ['OWNER', 'MEMBER'];

  isServerFFA = false;


  constructor(private dialog: DialogService, private createDialog: MatDialog, private nodeApi: NodesApiService) {
  }

  ngOnInit(): void {
    this.maxMemory = (this.node.memInfo.memoryTotal / 1024 / 1024);
    this.maxGpus = this.node.gpuInfo.length;
    this.nodeApi.getNodeResourceUsageConfiguration(this.node.name)
      .then((result) => {
        this.nodeResourceAllocations = JSON.parse(JSON.stringify((result)));
        this.editableResourceAllocations = JSON.parse(JSON.stringify((result)));
      });
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
          alreadyAssigned: this.nodeResourceAllocations.groupLimits
        } as unknown as AddGroupData
      })
      .afterClosed()
      .subscribe((data) => {
        if (data) {
          const updateObject: NodeResourceInfo = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
          const groupToAdd = {
            name: data.groupName,
            role: data.groupRole,
            resourceLimitation : {
              cpu: 0,
              mem: 0,
              gpu: 0
            }
          };
          updateObject.groupLimits.push(groupToAdd);
          return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(updateObject, this.node.name)
              .then(() => {
                this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
                this.editableResourceAllocations = JSON.parse(JSON.stringify(updateObject));
              }),
            'Adding Group to Resource Allocation');
        }
      });
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
    this.editableResourceAllocations.globalLimit.cpu = event.value;
  }

  gpuHasChanged(event, group) {
    group.resourceLimitation.gpu = event.value;
  }
  gpuHasChangedFFA(event) {
    this.editableResourceAllocations.globalLimit.gpu = event.value;
  }

  memorySliderHasChanged(event, group) {
    group.resourceLimitation.mem = event.value;
  }
  memorySliderHasChangedFFA(event) {
    this.editableResourceAllocations.globalLimit.mem = event.value;
  }
  memoryInputHasChanged(event) {
    if (0 <= event.target.value && event.target.value <= this.maxMemory) {
      this.editableResourceAllocations.globalLimit.mem = event.target.value;
    }
  }
  memoryInputHasChangedFFA(event) {
    if (0 <= event.target.value && event.target.value <= this.maxMemory) {
      this.editableResourceAllocations.globalLimit.mem = event.target.value;
    }
  }
  getMemoryString(mem) {
    if (mem >= 1024) {
      return (mem / 1024).toFixed(2) + ' GiB';
    } else {
      return mem + ' MiB';
    }
  }


  roleChanged(group) {
    const updateObject: NodeResourceInfo = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    const groupIndex: number = this.findGroupIndex(updateObject, group);
    updateObject.groupLimits[groupIndex].role = group.role;
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(updateObject, this.node.name)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Changing groups role');
  }
  updateGroup(group) {
    const updateObject: NodeResourceInfo = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    const groupIndex: number = this.findGroupIndex(updateObject, group);
    updateObject.groupLimits[groupIndex] = group;
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(updateObject, this.node.name)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Updating Group Resource Allocations');
  }

  updateResourcesFFA() {
    const updateObject: NodeResourceInfo = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    updateObject.globalLimit = this.editableResourceAllocations.globalLimit;
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(updateObject, this.node.name)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
          this.editableResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Updating Global Resource Limit');
  }

  onRemoveItemClick(group) {

    const updateObject: NodeResourceInfo = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    const groupIndex = this.findGroupIndex(updateObject, group);
    updateObject.groupLimits.splice(groupIndex, 1);
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(updateObject, this.node.name)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
          this.editableResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Updating Group Resource Allocations');
  }

  findGroupIndex(object: NodeResourceInfo, group) {
    let groupIndex = 0;
    object.groupLimits.find((g, i) => {
      if (g.name === group.name) {
        groupIndex = i;
      }
    });
    return groupIndex;
  }

  ffaHasChanged(event) {
    const updateObject: NodeResourceInfo = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    updateObject.freeForAll = event.checked;
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(updateObject, this.node.name)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
          this.editableResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Updating Servers FFA status');
  }

}
