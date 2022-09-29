import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-groups-view',
  templateUrl: './groups-view.component.html',
  styleUrls: ['./groups-view.component.css']
})
export class GroupsViewComponent implements OnInit {
  groupName = '';
  showGroupDropdown = false;

  constructor() { }

  ngOnInit(): void {
  }
  onDropdownToggle() {
    this.showGroupDropdown = !this.showGroupDropdown;
  }
  filterFunction() {
    console.log('Filter');
  }
  onItemClick() {
    console.log('Item Click');
  }

}
