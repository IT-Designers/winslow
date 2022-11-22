import {Component, OnInit, Input, Output, EventEmitter, SimpleChanges, OnChanges} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {NewGroupDialogComponent} from '../new-group-dialog/new-group-dialog.component';
import {UserAddNameDialogComponent} from '../user-add-name-dialog/user-add-name-dialog.component';

@Component({
  selector: 'app-searchable-list',
  templateUrl: './searchable-list.component.html',
  styleUrls: ['./searchable-list.component.css']
})
export class SearchableListComponent implements OnInit, OnChanges {

  @Input() type = 'none';
  @Input() allItems: object[] = [];
  @Input() searchPlaceholderText = 'Search...';
  @Input() listItemTooltip = 'Edit';

  @Output() itemEmitter = new EventEmitter();
  @Output() newItemEmitter = new EventEmitter();

  displayItems: object[];
  selectedItemName = '';
  itemSearchInput: string;
  showSystemGroups = false;

  constructor(private createDialog: MatDialog) {
  }

  ngOnInit(): void {
  }

  ngOnChanges(changes: SimpleChanges) {
    this.displayItems = Array.from(this.allItems);
    this.filterSystemGroups();
    this.sortDisplayItemsByName();
  }

  filterSystemGroups() {
    if (!this.showSystemGroups) {
      let i = 0;
      this.displayItems = Array.from(this.allItems);
      // this.sortDisplayItemsByName();
      for (const item of this.displayItems) {
        // @ts-ignore
        // @ts-ignore
        if (item.name.includes('::')) {
          this.displayItems.splice(i, 1);
          /*i--;*/
        }
        i++;
      }
      this.sortDisplayItemsByName();
    } else if (this.showSystemGroups) {
      this.displayItems = Array.from(this.allItems);
      this.sortDisplayItemsByName();
    }
  }

  sortDisplayItemsByName() {
    this.displayItems.sort((a, b) => {
      // @ts-ignore
      if (a.name.toUpperCase() > b.name.toUpperCase()) {
        return 1;
      } else {
        return -1;
      }
    });
  }

  searchItemFilter() {
    this.displayItems = Array.from(this.allItems);
    let searchedItems = Array.from(this.displayItems);
    if (this.itemSearchInput !== '') {
      searchedItems = [];
      for (const item of this.displayItems) {
        // @ts-ignore
        if (item.name.toUpperCase().includes(this.itemSearchInput.toUpperCase())) {
          searchedItems.push(item);
        }
      }
      this.displayItems = Array.from(searchedItems);
      this.sortDisplayItemsByName();
    }
  }

  itemClicked(item) {
    console.dir(this.displayItems);
    this.selectedItemName = item.name;
    this.itemEmitter.emit(item);
  }

  newBtnClicked() {
    console.log('Button clicked');
    if (this.type === 'Group') {
      this.createDialog.open(NewGroupDialogComponent, {
        data: {} as string
      })
        .afterClosed()
        .subscribe((name) => {
          this.selectedItemName = name;
          this.newItemEmitter.emit(name);
        });
    } else if (this.type === 'User') {
      this.createDialog.open(UserAddNameDialogComponent, {
        data: {} as string
      })
        .afterClosed()
        .subscribe((name) => {
          this.selectedItemName = name;
          this.newItemEmitter.emit(name);
        });
    }
  }
}
