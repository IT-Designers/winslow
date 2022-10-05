import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-groups-view',
  templateUrl: './groups-view.component.html',
  styleUrls: ['./groups-view.component.css']
})
export class GroupsViewComponent implements OnInit {
  groupName = '';
  showAddGroup = false;
  showMemberDropdown = false;
  showNewMemberDropdown = false;
  showOwnerDropdown = false;
  showNewOwnerDropdown = false;

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
    console.dir(this.mockGroups);
  }
  onDropdownToggle() {
    this.showMemberDropdown = !this.showMemberDropdown;
    if (!this.showMemberDropdown) {
      this.showNewMemberDropdown = false;
    }
  }
  onAddMemberClick() {
    this.showNewMemberDropdown = !this.showNewMemberDropdown;
  }
  onOwnerDropdownToggle() {
    this.showOwnerDropdown = !this.showOwnerDropdown;
    if (!this.showOwnerDropdown) {
      this.showNewOwnerDropdown = false;
    }
  }
  onAddOwnerClick() {
    this.showNewOwnerDropdown = !this.showNewOwnerDropdown;
  }
  onItemClick(user) {
    console.log('User: ' + user.name + ' has been clicked');
  }
  onAddGroupToggle() {
    console.log('Group toggle');
    this.showAddGroup = !this.showAddGroup;
    if (this.showAddGroup) {
      this.showGroupDetail = false;
    }
  }
  filterFunction(elementName: string, searchTextId: string) {
    let input;
    let filter;
    let divs;
    let i;
    input = document.getElementById(searchTextId);
    filter = input.value.toUpperCase();
    divs = document.getElementsByName(elementName);
    for (i = 0; i < divs.length; i++) {
      const txtValue = divs[i].textContent || divs[i].innerText;
      if (txtValue.toUpperCase().indexOf(filter) > -1) {
        divs[i].style.display = '';
      } else {
        divs[i].style.display = 'none';
      }
    }
  }
  onRemoveMemberClick(user) {
    const delIndex = this.mockMembers.findIndex((tempUser) => tempUser.id === user.id);
    this.mockUsers.push(this.mockMembers[delIndex]);
    this.mockMembers.splice(delIndex, 1);
    this.mockUsers.sort((a, b) => a.id - b.id);
  }
  onAddUserAsMemberClick(user) {
    const addIndex = this.mockUsers.findIndex((tempUser) => tempUser.id === user.id);
    this.mockMembers.push(this.mockUsers[addIndex]);
    this.mockUsers.splice(addIndex, 1);
    this.mockMembers.sort((a, b) => a.id - b.id);
  }
  onRemoveOwnerClick(user) {
    const delIndex = this.mockOwners.findIndex((tempUser) => tempUser.id === user.id);
    this.mockUsers.push(this.mockOwners[delIndex]);
    this.mockOwners.splice(delIndex, 1);
    this.mockUsers.sort((a, b) => a.id - b.id);
  }
  onAddUserAsOwnerClick(user) {
    const addIndex = this.mockUsers.findIndex((tempUser) => tempUser.id === user.id);
    this.mockOwners.push(this.mockUsers[addIndex]);
    this.mockUsers.splice(addIndex, 1);
    this.mockOwners.sort((a, b) => a.id - b.id);
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
    this.showOwnerDropdown = false;
    this.showNewOwnerDropdown = false;
    this.showMemberDropdown = false;
    this.showNewMemberDropdown = false;
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
