import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-groups-view',
  templateUrl: './groups-view.component.html',
  styleUrls: ['./groups-view.component.css']
})
export class GroupsViewComponent implements OnInit {
  groupName = '';
  showAddGroup = false;

  mockUsers = [
    {name: 'User 2', id: 2},
    {name: 'User 4', id: 4},
    {name: 'User 6', id: 6},
    {name: 'User 7', id: 7},
    {name: 'User 8', id: 8},
    {name: 'User 9', id: 9},
    {name: 'User 10', id: 10},
  ];
  mockMembers = [
    {name: 'Kyle Mezger', id: 1},
    {name: 'Rico Hoffmann', id: 3},
    {name: 'Michael Watzko', id: 5}
  ];
  mockOwners = [
    {name: 'Kyle Mezger', id: 1}
  ];

  mockGroups = [
    {
      name: 'Gruppe 1',
      owners: [
        {name: 'User 1', id: 1},
        {name: 'User 2', id: 2},
        {name: 'User 3', id: 3},
        {name: 'User 4', id: 4},
        {name: 'User 5', id: 5}
      ],
      members: [
        {name: 'User 1', id: 1},
        {name: 'User 2', id: 2},
        {name: 'User 3', id: 3},
        {name: 'User 4', id: 4},
        {name: 'User 5', id: 5}
      ]
    },
    {
      name: 'Gruppe 2',
      owners: [
        {name: 'User 2', id: 2},
      ],
      members: [
        {name: 'User 2', id: 2},
        {name: 'User 4', id: 4},
        {name: 'User 5', id: 5}
      ]
    },
    {
      name: 'Gruppe 3',
      owners: [
        {name: 'User 3', id: 3},
        {name: 'User 5', id: 5}
      ],
      members: [
        {name: 'User 1', id: 1},
        {name: 'User 2', id: 2},
        {name: 'User 3', id: 3},
        {name: 'User 5', id: 5}
      ]
    },
    {
      name: 'Gruppe 4',
      owners: [
        {name: 'User 1', id: 1},
        {name: 'User 2', id: 2},
        {name: 'User 4', id: 4},
      ],
      members: [
        {name: 'User 1', id: 1},
        {name: 'User 2', id: 2},
        {name: 'User 4', id: 4},
        {name: 'User 5', id: 5}
      ]
    },
    {
      name: 'Gruppe 5',
      owners: [
        {name: 'User 5', id: 5}
      ],
      members: [
        {name: 'User 5', id: 5}
      ]
    },
  ];
  showGroupDetail = false;
  selectedGroup = {name: 'No Group Selected', owners: [], members: []};

  constructor() { }

  ngOnInit(): void {
  }
  onAddGroupToggle() {
    this.showAddGroup = !this.showAddGroup;
    if (this.showAddGroup) {
      this.showGroupDetail = false;
    }
  }
  onSaveGroup() {
    const newGroup = {
      name: this.groupName,
      owners: this.mockOwners,
      members: this.mockMembers,
    };
    this.mockGroups.push(newGroup);
    this.onCancel();
  }
  onCancel() {
    this.groupName = '';
    this.mockMembers = [];
    this.mockOwners = [];
    this.showAddGroup = false;
  }
  groupClicked(group) {
    this.selectedGroup = group;
    this.showGroupDetail = true;
    if (this.showGroupDetail) {
      this.showAddGroup = false;
    }
    /*this.showGroupDetail = !this.showGroupDetail;
    if (this.showGroupDetail) {
      this.selectedGroup = group;
    } else {
      this.selectedGroup = {name: 'No Group Selected', owners: [], members: []};
    }*/
  }
}
