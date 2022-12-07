import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-ressources-group-assignment',
  templateUrl: './ressources-group-assignment.component.html',
  styleUrls: ['./ressources-group-assignment.component.css']
})
export class RessourcesGroupAssignmentComponent implements OnInit {

  mockGroups = [
    {
      name: 'Group 1',
      role: 'OWNER'
    },
    {
      name: 'Group 2',
      role: 'MEMBER'
    }
  ];
  memorySliderValue = 0;
  memoryMin = 0;
  memoryMax = 32;
  constructor() { }

  ngOnInit(): void {
  }

  formatLabel(value: number) {
    return value;
  }

}
