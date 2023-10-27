import {Component, Input, OnChanges, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {
  AddGroupData,
  ProjectAddGroupDialogComponent
} from '../../project-view/project-add-group-dialog/project-add-group-dialog.component';
import {ErrorStateMatcher} from '@angular/material/core';
import {UntypedFormControl, FormGroupDirective, NgForm} from '@angular/forms';
import {GroupResourceLimitEntry, NodeResourceUsageConfiguration} from '../../api/winslow-api';
import {NodeInfoExt, NodesApiService} from '../../api/nodes-api.service';
import {DialogService} from '../../dialog.service';
import {UserApiService} from '../../api/user-api.service';
import {GroupApiService} from '../../api/group-api.service';
import {MatCheckboxChange} from "@angular/material/checkbox";

export interface AssignedGroupInfo {
  name: string;
  role: string;
}


export class MyErrorStateMatcher implements ErrorStateMatcher {
  isErrorState(control: UntypedFormControl | null, form: FormGroupDirective | NgForm | null): boolean {
    const isSubmitted = form && form.submitted;
    return !!(control && control.invalid && (control.dirty || control.touched || isSubmitted));
  }
}

@Component({
  selector: 'app-server-groups-list',
  templateUrl: './server-groups-list.component.html',
  styleUrls: ['./server-groups-list.component.css']
})
export class ServerGroupsListComponent implements OnInit, OnChanges {

  @Input() assignedGroups!: AssignedGroupInfo[];
  @Input() node!: NodeInfoExt;

  userIsAdmin = false;
  userGroups?: string[];
  userRole = 'MEMBER';

  defaultResourceObject = {
    freeForAll: false,
    globalLimit: {
      cpu: 0,
      gpu: 0,
      mem: 0
    },
    groupLimits: []
  };
  nodeResourceAllocations: NodeResourceUsageConfiguration = this.defaultResourceObject;
  editableResourceAllocations: NodeResourceUsageConfiguration = this.defaultResourceObject;

  maxMemory = 0;
  maxCpuCores = 0;
  maxGpus = 0;

  matcher = new MyErrorStateMatcher();

  groupSearchInput = '';
  displayGroups?: AssignedGroupInfo[];
  roles = ['OWNER', 'MEMBER'];

  isServerFFA = false;


  constructor(private dialog: DialogService,
              private createDialog: MatDialog,
              private nodeApi: NodesApiService,
              private userApi: UserApiService,
              private groupApi: GroupApiService) {
  }

  ngOnInit(): void {
    this.userApi.getSelfUserName()
      .then((name) => {
        this.userApi.hasSuperPrivileges(name)
          .then((bool) => {
            this.userIsAdmin = bool;
            this.groupApi.getGroups()
              .then((groups) => {
                this.userGroups = groups.map(x => x.name);
                this.isUserOwner();
              });
          });
      });
    this.maxCpuCores = this.node.cpuInfo.utilization.length;
    this.maxMemory = (this.node.memInfo.memoryTotal / 1024 / 1024);
    this.maxGpus = this.node.gpuInfo.length;
    this.nodeApi.getNodeResourceUsageConfiguration(this.node.name)
      .then((result) => {
        if (result !== null) {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify((result)));
          this.editableResourceAllocations = JSON.parse(JSON.stringify((result)));
        } else {
          this.nodeResourceAllocations = this.defaultResourceObject;
          this.editableResourceAllocations = this.defaultResourceObject;
        }

      });
    this.displayGroups = Array.from(this.assignedGroups);
  }

  ngOnChanges() {
    /*this.isUserOwner();*/
  }

  isUserOwner() {
    if (!this.userIsAdmin) {
      // tslint:disable-next-line:prefer-for-of
      for (let i = 0; i < this.nodeResourceAllocations.groupLimits.length; i++) {
        if (this.userGroups) {
          if (this.userGroups.includes(this.nodeResourceAllocations.groupLimits[i].name)) {
            if (this.nodeResourceAllocations.groupLimits[i].role === 'OWNER') {
              this.userRole = 'OWNER';
            } else {
              this.userRole = 'MEMBER';
            }
          } else {
            this.userRole = 'MEMBER';
          }
        }
      }
    } else {
      this.userRole = 'OWNER';
    }
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
          const updateObject: NodeResourceUsageConfiguration = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
          const groupToAdd = {
            name: data.groupName,
            role: data.groupRole,
            resourceLimitation: {
              cpu: 0,
              mem: 0,
              gpu: 0
            }
          };
          updateObject.groupLimits.push(groupToAdd);
          return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(this.node.name, updateObject)
              .then(() => {
                this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
                this.editableResourceAllocations = JSON.parse(JSON.stringify(updateObject));
              }),
            'Adding Group to Resource Allocation');
        }
      });
  }

  getChipColor(group: GroupResourceLimitEntry) {
    if (group.role === 'OWNER') {
      return '#8ed69b';
    } else {
      return '#d88bca';
    }
  }

  getTooltip(group: GroupResourceLimitEntry) {
    if (group.role === 'OWNER') {
      return 'OWNER';
    } else if (group.role === 'MEMBER') {
      return 'MEMBER';
    }
  }

  memoryInputHasChanged(event: Event) {
    if (!this.editableResourceAllocations.globalLimit) {
      console.error("Global limit is not defined!");
      return;
    }
    const target = event.target;
    if (!(target instanceof HTMLInputElement)) {
      return;
    }
    const value = Number(target.value);
    if (0 <= value && value <= this.maxMemory) {
      this.editableResourceAllocations.globalLimit.mem = value;
    }
  }

  getMemoryString(mem: number | undefined) {
    if (mem == undefined) {
      return "???"
    } else if (mem >= 1024) {
      return (mem / 1024).toFixed(2) + ' GiB';
    } else {
      return mem + ' MiB';
    }
  }


  roleChanged(group: GroupResourceLimitEntry) {
    const updateObject: NodeResourceUsageConfiguration = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    const groupIndex: number = this.findGroupIndex(updateObject, group);
    updateObject.groupLimits[groupIndex].role = group.role;
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(this.node.name, updateObject)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Changing groups role');
  }

  updateGroup(group: GroupResourceLimitEntry) {
    const updateObject: NodeResourceUsageConfiguration = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    const groupIndex: number = this.findGroupIndex(updateObject, group);
    updateObject.groupLimits[groupIndex] = group;
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(this.node.name, updateObject)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Updating Group Resource Allocations');
  }

  updateResourcesFFA() {
    const updateObject: NodeResourceUsageConfiguration = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    updateObject.globalLimit = this.editableResourceAllocations.globalLimit;
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(this.node.name, updateObject)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
          this.editableResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Updating Global Resource Limit');
  }

  onRemoveItemClick(group: GroupResourceLimitEntry) {

    const updateObject: NodeResourceUsageConfiguration = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    const groupIndex = this.findGroupIndex(updateObject, group);
    updateObject.groupLimits.splice(groupIndex, 1);
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(this.node.name, updateObject)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
          this.editableResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Updating Group Resource Allocations');
  }

  findGroupIndex(object: NodeResourceUsageConfiguration, group: GroupResourceLimitEntry) {
    let groupIndex = 0;
    object.groupLimits.find((g, i) => {
      if (g.name === group.name) {
        groupIndex = i;
      }
    });
    return groupIndex;
  }

  ffaHasChanged(event: MatCheckboxChange) {
    const updateObject: NodeResourceUsageConfiguration = JSON.parse(JSON.stringify(this.nodeResourceAllocations));
    updateObject.freeForAll = event.checked;
    return this.dialog.openLoadingIndicator(this.nodeApi.setNodeResourceUsageConfiguration(this.node.name, updateObject)
        .then(() => {
          this.nodeResourceAllocations = JSON.parse(JSON.stringify(updateObject));
          this.editableResourceAllocations = JSON.parse(JSON.stringify(updateObject));
        }),
      'Updating Servers FFA status');
  }

}
