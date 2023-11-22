import {Component, OnInit} from '@angular/core';
import {FormControl} from "@angular/forms";
import {Observable} from "rxjs";
import {map, startWith} from "rxjs/operators";
import {MatDialog} from "@angular/material/dialog";

@Component({
  selector: 'app-projects-view-filter',
  templateUrl: './projects-view-filter.component.html',
  styleUrl: './projects-view-filter.component.css'
})
export class ProjectsViewFilterComponent implements OnInit {

  myControl = new FormControl('');
  options: string[] = ['One', 'Two', 'Three'];
  filteredOptions: Observable<string[]> | undefined;

  constructor(
    private dialog: MatDialog,
  ) {
  }

  ngOnInit() {
    this.filteredOptions = this.myControl.valueChanges.pipe(
      startWith(''),
      map(value => this._filter(value || '')),
    );
  }

  private _filter(value: string): string[] {
    const filterValue = value.toLowerCase();

    return this.options.filter(option => option.toLowerCase().includes(filterValue));
  }

  private openAdvancedFilterOptions() {
    // this.dialog.open();
  }

}
