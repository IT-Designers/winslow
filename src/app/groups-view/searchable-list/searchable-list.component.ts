import {Component, OnInit, Input, Output, EventEmitter, SimpleChanges, OnChanges} from '@angular/core';

@Component({
  selector: 'app-searchable-list',
  templateUrl: './searchable-list.component.html',
  styleUrls: ['./searchable-list.component.css']
})
export class SearchableListComponent implements OnInit, OnChanges {

  @Input() allItems: object[] = [];
  @Input() searchPlaceholderText = 'Search...';

  @Output() itemEmitter = new EventEmitter();

  displayItems: object[];
  selectedItem: object;
  itemSearchInput: string;
  constructor() { }

  ngOnInit(): void {
    console.dir(this.allItems);
  }

  ngOnChanges(changes: SimpleChanges) {
    this.displayItems = Array.from(this.allItems);
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
    this.selectedItem = item;
    this.itemEmitter.emit(item);
  }

}
