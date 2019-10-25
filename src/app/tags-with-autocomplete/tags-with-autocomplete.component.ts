import {Component, ElementRef, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {FormControl} from '@angular/forms';
import {COMMA, ENTER} from '@angular/cdk/keycodes';
import {Observable} from 'rxjs';
import {MatAutocomplete, MatAutocompleteSelectedEvent, MatChipInputEvent} from '@angular/material';
import {map, startWith} from 'rxjs/operators';

@Component({
  selector: 'app-tags-with-autocomplete',
  templateUrl: './tags-with-autocomplete.component.html',
  styleUrls: ['./tags-with-autocomplete.component.css']
})
export class TagsWithAutocompleteComponent implements OnInit {

  selectable = true;
  removable = true;
  tagsCtrl = new FormControl();
  separatorKeysCodes: number[] = [ENTER, COMMA];
  addOnBlur = true;

  @Input() colorful = true;
  @Input() readonly = false;
  @Input() unique = true;
  @Input() sort = true;
  @Input() proposals: string[] = [];

  selectedTags: string[] = [];
  filteredTags: Observable<string[]>;

  @ViewChild('tagInput', {static: false}) tagInput: ElementRef<HTMLInputElement>;
  @ViewChild('auto', {static: false}) matAutocomplete: MatAutocomplete;

  @Output('tags') tagsEmitter = new EventEmitter<string[]>();

  constructor() {
    this.filteredTags = this.tagsCtrl.valueChanges.pipe(
      startWith(null),
      map((tag: string | null) => tag ? this.filter(tag) : this.filter(''))
    );
  }

  @Input() // ensure this is working on a copy
  set tags(tags: string[]) {
    this.selectedTags = [];
    tags.forEach(tag => this.selectedTags.push(tag));
    if (this.sort) {
      this.sortSelectedTags();
    }
  }

  ngOnInit() {
    if (this.readonly) {
      this.tagsCtrl.disable();
    }
  }

  push(value: string) {
    if ((value || '').trim() && (!this.unique || this.selectedTags.indexOf(value.trim()) < 0)) {
      this.selectedTags.push(value.trim());
      if (this.sort) {
        this.sortSelectedTags();
      }
      this.tagsEmitter.emit(this.selectedTags);
    }
  }

  private sortSelectedTags() {
    this.selectedTags = this.selectedTags.sort((a, b) => a.localeCompare(b));
  }

  add($event: MatChipInputEvent) {
    if (!this.matAutocomplete.isOpen) {
      const input = $event.input;
      const value = $event.value;

      this.push(value);

      if (input) {
        input.value = '';
      }

      this.tagsCtrl.setValue(null);
    }
  }

  remove(tag: string) {
    const index = this.selectedTags.indexOf(tag);
    if (index >= 0) {
      this.selectedTags.splice(index, 1);
      this.tagsEmitter.emit(this.selectedTags);
    }
  }

  selected($event: MatAutocompleteSelectedEvent) {
    this.push($event.option.viewValue);
    this.tagsCtrl.setValue(null);
  }

  filter(tag: string) {
    const value = tag.toLowerCase();
    return this.proposals
      .filter(t => t.toLowerCase().indexOf(value) === 0
        && (!this.unique || this.selectedTags.indexOf(t) < 0)
      );
  }

  bgColor(tag: string) {
    tag = tag.trim();
    let sum = tag.length;
    for (let i = 0; i < tag.length; ++i) {
      sum += (i + 1) * tag.charCodeAt(i) * 1337;
    }

    const min = 128;
    const max = 256 - min;

    const red = ((sum / 7) % max) + min;
    const green = ((sum / 5) % max) + min;
    const blue = ((sum / 3) % max) + min;
    return `rgba(${red}, ${green}, ${blue}, 0.8)`;
  }
}
