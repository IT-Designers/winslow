import {Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {MatDialog} from "@angular/material/dialog";
import {ProjectInfo} from "../../api/winslow-api";
import {ProjectGroup} from "../../api/project-api.service";
import {LocalStorageService} from "../../api/local-storage.service";
import {FormControl} from "@angular/forms";
import {MatAutocomplete, MatAutocompleteSelectedEvent} from "@angular/material/autocomplete";
import {COMMA, ENTER} from "@angular/cdk/keycodes";
import {MatChipInputEvent} from "@angular/material/chips";
import {Observable, of} from "rxjs";
import {TagFilterComponent} from "../tag-filter/tag-filter.component";

export class SelectedTags {
  includedTags: string[] = []
  excludedTags: string[] = []
}

@Component({
  selector: 'app-projects-view-filter',
  templateUrl: './projects-view-filter.component.html',
  styleUrl: './projects-view-filter.component.css'
})
export class ProjectsViewFilterComponent implements OnInit {

  @ViewChild('auto') matAutocomplete!: MatAutocomplete;

  // Inputs
  @Input()
  set preSelectedTag(tag: string | undefined) {
    if (this.lastPreselectedTag) {
      this.removeIncludedTag(this.lastPreselectedTag);
    }
    if (tag) {
      this.addIncludedTag(tag);
      this.lastPreselectedTag = tag;
    }
    this.updateProjectsList();
  }

  @Input('projects')
  set projects(projects: ProjectInfo[]) {
    this.projectsValue = projects;
    this.updateProjectsList();
  }

  @Input('availableTags')
  set availableTags(tags: string[]) {
    this.availableTagsValue = tags;
    this._availableInTagsValue = of(tags);
    this._availableExTagsValue = of(tags);
    this.selectedTags.includedTags.forEach(tag => this.addIncludedTag(tag));
    this.selectedTags.excludedTags.forEach(tag => this.addExcludedTag(tag));
    this.updateProjectsList();
  }

  // ---

  // Variables
  CONTEXT_PREFIX = 'context::';
  TAG_PREFIX = '#';
  TAG_EXCLUDE_PREFIX = '-#';
  selectedTags: SelectedTags = new SelectedTags();
  _availableInTagsValue: Observable<string[]> = new Observable<string[]>();
  _availableExTagsValue: Observable<string[]> = new Observable<string[]>();
  availableTagsValue: string[] = [];
  lastPreselectedTag?: string;
  searchInputCtrl = new FormControl('');
  separatorKeysCodes: number[] = [ENTER, COMMA];
  projectsValue!: ProjectInfo[];
  filteredProjects!: ProjectInfo[];
  projectsGroupsValue?: ProjectGroup[];
  groupsOnTopIsChecked!: boolean;
  // ---

  // Output
  @Output('filtered') filteredProjectsOutput = new EventEmitter<ProjectInfo[] | undefined>();
  @Output('projectsGroups') projectsGroups = new EventEmitter<ProjectGroup[]>();
  @Output('groupsOnTop') groupsOnTop = new EventEmitter<boolean>();

  // ---


  constructor(
    private dialog: MatDialog,
    private localStorageService: LocalStorageService,
  ) {
  }

  ngOnInit() {
    this.groupsOnTopIsChecked = this.localStorageService.getGroupsOnTop() ?? false;
    if (this.localStorageService.getSelectedContext() !== '' && this.localStorageService.getSelectedContext() != null) {
      this.preSelectedTag = this.CONTEXT_PREFIX + this.localStorageService.getSelectedContext();
      if (this.preSelectedTag) {
        this.addIncludedTag(this.preSelectedTag);
      }
    }
    this.updateProjectsList();
  }

  // tag functions from old component
  toggleIncludedTag(tag: string) {
    if (this.selectedTags.includedTags != null) {
      const index = this.selectedTags.includedTags.indexOf(tag);
      if (index < 0) {
        const tags = this.selectedTags.includedTags.map(t => t);
        tags.push(tag);
        this.selectedTags.includedTags = tags; // notify the bindings
      } else {
        const tags = this.selectedTags.includedTags.map(t => t);
        tags.splice(index, 1);
        this.selectedTags.includedTags = tags; // notify the bindings
      }
      this.updateProjectsList();
    }
  }

  addIncludedTag(tag: string) {
    if (this.selectedTags.includedTags != null && this.selectedTags.includedTags.indexOf(tag) < 0) {
      let tags = this.selectedTags.includedTags.map(t => t);
      tags.push(tag);
      tags = tags.sort((a, b) => a.localeCompare(b));
      this.selectedTags.includedTags = tags; // notify the bindings
    }
    this.updateProjectsList();
  }

  removeIncludedTag(tag: string) {
    if (this.selectedTags.includedTags != null) {
      const index = this.selectedTags.includedTags.indexOf(tag);
      const tags = this.selectedTags.includedTags.map(t => t);
      tags.splice(index, 1);
      this.selectedTags.includedTags = tags; // notify the bindings
    }
    this.updateProjectsList();
  }

  addExcludedTag(tag: string) {
    if (this.selectedTags.excludedTags != null && this.selectedTags.excludedTags.indexOf(tag) < 0) {
      let tags = this.selectedTags.excludedTags.map(t => t);
      tags.push(tag);
      tags = tags.sort((a, b) => a.localeCompare(b));
      this.selectedTags.excludedTags = tags; // notify the bindings
    }
    this.updateProjectsList();
  }

  removeFromSelectedIncludedTag(tag: string) {
    const indexInclude = this.selectedTags.includedTags.indexOf(tag);
    if (indexInclude >= 0) {
      this.selectedTags.includedTags.splice(indexInclude, 1);
    }
    this.updateProjectsList();
  }

  removeFromSelectedExcludedTag(tag: string) {
    const indexExclude = this.selectedTags.excludedTags.indexOf(tag);
    if (indexExclude >= 0) {
      this.selectedTags.excludedTags.splice(indexExclude, 1);
    }
    this.updateProjectsList();
  }

  // ---


  // gets called with every keyup of the searchbar
  processInput() {
    console.log(this.searchInputCtrl.getRawValue());
    const input = this.searchInputCtrl.getRawValue();
    let lowercaseInput = input ? input.toLowerCase().trim() : '';
    if (lowercaseInput.startsWith(this.TAG_EXCLUDE_PREFIX)) {
      lowercaseInput = lowercaseInput.replace(this.TAG_EXCLUDE_PREFIX, '');
      this._availableExTagsValue = of(this.availableTagsValue
        .filter(value => {
          if (this.selectedTags.excludedTags.indexOf(value) < 0) { // isn't selected
            return value.toLowerCase().includes(lowercaseInput);
          }
        })
        .map(value => `-#${value}`)
      );
    } else if (lowercaseInput.startsWith(this.TAG_PREFIX)) {
      lowercaseInput = lowercaseInput.replace(this.TAG_PREFIX, '');
      this._availableInTagsValue = of(this.availableTagsValue
        .filter(value => {
          if (this.selectedTags.includedTags.indexOf(value) < 0) { // isn't selected
            return value.toLowerCase().includes(lowercaseInput);
          }
        })
        .map(value => `#${value}`)
      );
    } else {
      this.filteredProjects = this.getFilteredProjects();
      console.log(this.filteredProjects)
      this.filteredProjects = this.filteredProjects.filter(project => {
        return project.name.toLowerCase().includes(lowercaseInput) || project.name.toLowerCase().startsWith(lowercaseInput);
      })
      console.log(this.filteredProjects)
      this.filteredProjectsOutput.emit(this.filteredProjects);
      console.log(this.filteredProjects)
    }
    this.updateProjectsList();
  }

  updateProjectsList() {
    if (this.projectsValue == undefined) {
      this.filteredProjectsOutput.emit(undefined);
      return;
    }
    this.filteredProjects = this.getFilteredProjects();
    console.log("updateProjectList() emitted new List");
    this.filteredProjectsOutput.emit(this.filteredProjects);
  }

  getFilteredProjects() {
    return this.projectsValue.filter(project => {
      if (project.tags.length === 0) {
        return true;
      }

      if (this.selectedTags.includedTags.length > 0) {
        const hasIncludedTag = project.tags.some(tag => this.selectedTags.includedTags.includes(tag));
        if (!hasIncludedTag) {
          return false;
        }
      }

      if (this.selectedTags.excludedTags.length > 0) {
        const hasExcludedTag = project.tags.some(tag => this.selectedTags.excludedTags.includes(tag));
        if (hasExcludedTag) {
          return false;
        }
      }

      return true;
    });
  }

  applyFilterMiddleware(value: string) {
    let tag = value.trim() || '';
    if (tag.startsWith(this.TAG_PREFIX) && (this.selectedTags.includedTags.indexOf(tag) < 0)) {
      tag = tag.replace(this.TAG_PREFIX, '');
      this.addIncludedTag(tag);
    } else if (tag.startsWith(this.TAG_EXCLUDE_PREFIX) && (this.selectedTags.excludedTags.indexOf(tag) < 0)) {
      tag = tag.replace(this.TAG_EXCLUDE_PREFIX, '');
      this.addExcludedTag(tag);
    }
    this.updateProjectsList();
  }

  selectFromAutocomplete($event: MatAutocompleteSelectedEvent) {
    const selection = $event.option.viewValue;
    this.applyFilterMiddleware(selection);
  }

  selectFromInput($event: MatChipInputEvent) {
    const input = $event.input;
    const value = $event.value;
    if (value.startsWith(this.TAG_PREFIX) || value.startsWith(this.TAG_EXCLUDE_PREFIX)) {
      if (input) {
        input.value = '';
      }
      this.searchInputCtrl.setValue(null);
    }
  }

  openAdvancedFilterOptions() {
    this.dialog.open(TagFilterComponent);
  }

  emitGroups() {
    this.projectsGroups.emit(this.projectsGroupsValue);
    this.groupsOnTop.emit(this.groupsOnTopIsChecked);
  }
}
