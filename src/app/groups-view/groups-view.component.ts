import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-groups-view',
  templateUrl: './groups-view.component.html',
  styleUrls: ['./groups-view.component.css']
})
export class GroupsViewComponent implements OnInit {
  groupName = '';
  showAddGroup = true;
  showMemberDropdown = false;
  showNewMemberDropdown = false;
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
    {name: 'User 1', id: 1},
    {name: 'User 3', id: 3},
    {name: 'User 5', id: 5}
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
  onItemClick(user) {
    console.log('User: ' + user.name + ' has been clicked');
  }
  onAddGroupToggle() {
    console.log('Group toggle');
    this.showAddGroup = !this.showAddGroup;
  }
  filterFunction() {
    console.log('Filter Function');
    let input;
    let filter;
    let divs;
    let i;
    input = document.getElementById('memberDropdownSearchText');
    filter = input.value.toUpperCase();
    const div = document.getElementById('usersDropdown');
    divs = div.getElementsByTagName('dropdown-item');
    console.dir(divs);
    for (i = 0; i < divs.length; i++) {
      const txtValue = divs[i].textContent || divs[i].innerText;
      if (txtValue.toUpperCase().indexOf(filter) > -1) {
        divs[i].style.display = '';
      } else {
        divs[i].style.display = 'none';
      }
    }
  }
  onAddMemberClick() {
    this.showNewMemberDropdown = !this.showNewMemberDropdown;
  }
}
