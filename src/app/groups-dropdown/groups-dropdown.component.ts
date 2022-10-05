import { Component, OnInit, Input } from '@angular/core';

@Component({
  selector: 'app-groups-dropdown',
  templateUrl: './groups-dropdown.component.html',
  styleUrls: ['./groups-dropdown.component.css']
})
export class GroupsDropdownComponent implements OnInit {

  @Input() items = [];    // left dropdown
  @Input() items2 = [];   // right dropdown
  @Input() label = 'No Label:';
  @Input() buttonName = 'Öffnen';     // text in button to open dropdown
  @Input() openDropdown2Text = 'Hinzufügen';    // text on last item of left dropdown
  showDropdown = false;
  showDropdown2 = false;
  constructor() { }

  ngOnInit(): void {
  }
  onDropdownsToggle() {
    this.showDropdown = !this.showDropdown;
    if (!this.showDropdown) {
      this.showDropdown2 = false;
    }
  }
  filterFunction(inputId: string, divName: string) {
    let input;
    let filter;
    let divs;
    let i;
    input = document.getElementById(inputId);
    filter = input.value.toUpperCase();
    divs = document.getElementsByName(divName);
    for (i = 0; i < divs.length; i++) {
      const txtValue = divs[i].textContent || divs[i].innerText;
      if (txtValue.toUpperCase().indexOf(filter) > -1) {
        divs[i].style.display = '';
      } else {
        divs[i].style.display = 'none';
      }
    }
  }
  onRemoveItemClick(item) {
    const delIndex = this.items.findIndex((tempUser) => tempUser.id === item.id);
    this.items2.push(this.items[delIndex]);
    this.items.splice(delIndex, 1);
    this.items2.sort((a, b) => a.id - b.id);
  }
  onAddItemClick(item) {
    const addIndex = this.items2.findIndex((tempUser) => tempUser.id === item.id);
    this.items.push(this.items2[addIndex]);
    this.items2.splice(addIndex, 1);
    this.items.sort((a, b) => a.id - b.id);
  }
  onOpenSecondDropdownClick() {
    this.showDropdown2 = !this.showDropdown2;
  }
}
