import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, ViewChild} from '@angular/core';
import {FormControl} from '@angular/forms';
import {COMMA, ENTER} from '@angular/cdk/keycodes';
import {Observable} from 'rxjs';
import {MatAutocomplete, MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {map, startWith} from 'rxjs/operators';
import {MatChipInputEvent} from '@angular/material/chips';
import {Group, ProjectInfo} from '../../api/project-api.service';

@Component({
  selector: 'app-group-tags-with-autocomplete',
  templateUrl: './group-tags-with-autocomplete.component.html',
  styleUrls: ['./group-tags-with-autocomplete.component.css']
})
export class GroupTagsWithAutocompleteComponent implements OnInit, OnChanges {

  allGroups = [];
  groupsToDelete = [];

  selectable = true;
  removable = true;
  tagsCtrl = new FormControl();
  separatorKeysCodes: number[] = [ENTER, COMMA];
  addOnBlur = true;

  CONTEXT_PREFIX = 'context::';

  @Input() colorful = true;
  @Input() readonly = false;
  @Input() isFilter = false;
  @Input() unique = true;
  @Input() sort = true;
  @Input() proposals: string[] = [];
  @Input() project: ProjectInfo;

  selectedTags: string[] = [];
  filteredTags: Observable<string[]>;

  @ViewChild('auto') matAutocomplete: MatAutocomplete;

  @Output('groupTags') tagsEmitter = new EventEmitter<string[]>();
  @Output() tagActionPrimary = new EventEmitter<string>();
  @Output() tagActionSecondary = new EventEmitter<string>();

  constructor() {
    this.filteredTags = this.tagsCtrl.valueChanges.pipe(
      startWith(null),
      map((tag: string | null) => {
        const value = tag ? tag.toLowerCase() : '';
        if (this.isFilter) {
          return this.proposals
            .filter(t => t.toLowerCase().indexOf(value) === 0
              && (!this.unique || this.selectedTags.indexOf(t) < 0)
              && (!t.startsWith(this.CONTEXT_PREFIX))
            );
        } else {
          return this.proposals
            .filter(t => t.toLowerCase().indexOf(value) === 0
              && (!this.unique || this.selectedTags.indexOf(t) < 0)
            );
        }
      })
    );
  }
  @Input() // ensure this is working on a copy
  set groupTags(groupTags: string[]) {
   /* this.selectedTags = [];
    /!*this.selectedTags = this.groupObjects.map((group) => group.name);*!/
    if (this.readonly) {
      this.selectedTags = groupTags.filter(tag => !tag.startsWith(this.CONTEXT_PREFIX));
    } else {
      if (this.isFilter) {
        this.selectedTags = groupTags.filter(tag => !tag.startsWith(this.CONTEXT_PREFIX));
      } else {
        // this.selectedTags = groupTags.map(tag => tag);
        /!*let test = [];
        if (this.project) {
          test = this.project.groups.map((group) => group.name);
          console.dir(test);
        }*!/
      }
    }
    if (this.sort) {
      this.sortSelectedTags();
    }*/
    // console.dir(this.selectedTags);
  }

  ngOnInit() {
    if (this.readonly) {
      this.tagsCtrl.disable();
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    /*console.dir(this.project);
    this.selectedTags = this.project.groups.map(group => group.name);*/
  }

  getColor(group) {
    if (group.role === 'OWNER') {
      return '#8ed69b';
    } else {
      return '#d88bca';
    }
  }

  push(value: string) {
    /*if ((value || '').trim() && (!this.unique || this.selectedTags.indexOf(value.trim()) < 0)) {
      this.selectedTags.push(value.trim());
      if (this.sort) {
        this.sortSelectedTags();
      }
      this.tagsEmitter.emit(this.selectedTags);
    }*/
  }

  private sortSelectedTags() {
/*
    this.selectedTags = this.selectedTags.sort((a, b) => a.localeCompare(b));
*/
  }

  add($event: MatChipInputEvent) {
    /*if (!this.matAutocomplete.isOpen) {
      const input = $event.input;
      const value = $event.value;

      this.push(value);

      if (input) {
        input.value = '';
      }

      this.tagsCtrl.setValue(null);
    }*/
  }

  remove(tag: string) {
    /*this.groupsToDelete.push(tag);
    const index = this.selectedTags.indexOf(tag);
    if (index >= 0) {
      this.selectedTags.splice(index, 1);
    }*/
  }

  selected($event: MatAutocompleteSelectedEvent) {
    /*this.push($event.option.viewValue);
    this.tagsCtrl.setValue(null);*/
  }

  bgColor(tag: string) {
    /*tag = tag?.trim();
    let sum = tag?.length;
    for (let i = 0; i < tag?.length; ++i) {
      sum += (i + 1) * tag?.charCodeAt(i) * 1337;
    }

    const min = 128;
    const max = 256 - min;

    const red = ((sum / 7) % max) + min;
    const green = ((sum / 5) % max) + min;
    const blue = ((sum / 3) % max) + min;
    return `rgba(${red}, ${green}, ${blue}, 0.8)`;*/
  }

}
