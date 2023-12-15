import {ApplicationRef, Component, EventEmitter, Input, OnInit, Output, ViewChild} from '@angular/core';
import {ProjectInfo} from "../../api/winslow-api";
import {ProjectGroup} from "../../api/project-api.service";
import {LocalStorageService} from "../../api/local-storage.service";
import {FormControl} from "@angular/forms";
import {MatAutocomplete, MatAutocompleteSelectedEvent} from "@angular/material/autocomplete";
import {COMMA, ENTER} from "@angular/cdk/keycodes";
import {MatChipInputEvent} from "@angular/material/chips";
import {Observable, of} from "rxjs";

export class SelectedFilters {
  includedTags: string[] = []
  excludedTags: string[] = []
  includedPipelines: string[] = []
}

@Component({
  selector: './app-projects-view-filter',
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
      this.lastPreselectedTag = undefined;
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
    this.availablePipelinesValue = [];
    projects.forEach(project => {
      if (!this.availablePipelinesValue.includes(project.pipelineDefinition.name)) {
        this.availablePipelinesValue.push(project.pipelineDefinition.name)
      }
    });
    this._availablePipelinesValue = of(this.availablePipelinesValue);
    this.updateProjectsList();
  }

  @Input('availableTags')
  set availableTags(tags: string[]) {
    this.availableTagsValue = tags;
    this._availableInTagsValue = of(tags);
    this._availableExTagsValue = of(tags);
    this.updateProjectsList();
  }

  // ---

  // Variables
  CONTEXT_PREFIX = 'context::';
  TAG_PREFIX = '#';
  TAG_EXCLUDE_PREFIX = '-#';
  PIPELINE_PREFIX = 'pipeline:';
  selectedFilters: SelectedFilters = new SelectedFilters();
  _availableInTagsValue: Observable<string[]> = new Observable<string[]>();
  _availableExTagsValue: Observable<string[]> = new Observable<string[]>();
  _availablePipelinesValue: Observable<string[]> = new Observable<string[]>();
  availablePipelinesValue: string [] = [];
  availableTagsValue: string[] = [];
  lastPreselectedTag?: string;
  searchInputCtrl = new FormControl('');
  separatorKeysCodes: number[] = [ENTER, COMMA];
  projectsValue!: ProjectInfo[];
  filteredProjects!: ProjectInfo[];
  projectsGroupsValue?: ProjectGroup[];
  groupsOnTopIsChecked!: boolean;
  showExtendedOptions: boolean = true;
  // ---

  // Output
  @Output('filtered') filteredProjectsOutput = new EventEmitter<ProjectInfo[] | undefined>();
  @Output('projectsGroups') projectsGroups = new EventEmitter<ProjectGroup[]>();
  @Output('groupsOnTop') groupsOnTop = new EventEmitter<boolean>();

  // ---


  constructor(
    private localStorageService: LocalStorageService,
    private appRef: ApplicationRef,
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
    this.selectedFilters = this.localStorageService.getSelectedFilters();
    this.updateProjectsList();
  }

  // tag functions from old component
  toggleIncludedTag(tag: string) {
    if (this.selectedFilters.includedTags != null) {
      const index = this.selectedFilters.includedTags.indexOf(tag);
      if (index < 0) {
        const tags = this.selectedFilters.includedTags.map(t => t);
        tags.push(tag);
        this.selectedFilters.includedTags = tags; // notify the bindings
      } else {
        const tags = this.selectedFilters.includedTags.map(t => t);
        tags.splice(index, 1);
        this.selectedFilters.includedTags = tags; // notify the bindings
      }
      this.updateProjectsList();
      this.localStorageService.setSelectedFilters(this.selectedFilters);
    }
  }

  addIncludedTag(tag: string) {
    if (this.selectedFilters.includedTags != null && this.selectedFilters.includedTags.indexOf(tag) < 0) {
      let tags = this.selectedFilters.includedTags.map(t => t);
      tags.push(tag);
      tags = tags.sort((a, b) => a.localeCompare(b));
      this.selectedFilters.includedTags = tags; // notify the bindings
      this.localStorageService.setSelectedFilters(this.selectedFilters);
    }
    this.updateProjectsList();
  }

  removeIncludedTag(tag: string) {
    if (this.selectedFilters.includedTags != null) {
      const index = this.selectedFilters.includedTags.indexOf(tag);
      const tags = this.selectedFilters.includedTags.map(t => t);
      tags.splice(index, 1);
      this.selectedFilters.includedTags = tags; // notify the bindings
      this.localStorageService.setSelectedFilters(this.selectedFilters);
    }
    this.updateProjectsList();
  }

  addExcludedTag(tag: string) {
    if (this.selectedFilters.excludedTags != null && this.selectedFilters.excludedTags.indexOf(tag) < 0) {
      let tags = this.selectedFilters.excludedTags.map(t => t);
      tags.push(tag);
      tags = tags.sort((a, b) => a.localeCompare(b));
      this.selectedFilters.excludedTags = tags; // notify the bindings
      this.localStorageService.setSelectedFilters(this.selectedFilters);
    }
    this.updateProjectsList();
  }

  removeFromSelectedIncludedTag(tag: string) {
    const indexInclude = this.selectedFilters.includedTags.indexOf(tag);
    if (indexInclude >= 0) {
      this.selectedFilters.includedTags.splice(indexInclude, 1);
      this.localStorageService.setSelectedFilters(this.selectedFilters);
    }
    this.updateProjectsList();
  }

  removeFromSelectedExcludedTag(tag: string) {
    const indexExclude = this.selectedFilters.excludedTags.indexOf(tag);
    if (indexExclude >= 0) {
      this.selectedFilters.excludedTags.splice(indexExclude, 1);
      this.localStorageService.setSelectedFilters(this.selectedFilters);
    }
    this.updateProjectsList();
  }

  addIncludedPipeline(pipeline: string) {
    if (this.selectedFilters.includedPipelines != null && this.selectedFilters.includedPipelines.indexOf(pipeline) < 0) {
      let pipelines = this.selectedFilters.includedPipelines.map(t => t);
      pipelines.push(pipeline);
      pipelines = pipelines.sort((a, b) => a.localeCompare(b));
      this.selectedFilters.includedPipelines = pipelines; // notify the bindings
      this.localStorageService.setSelectedFilters(this.selectedFilters);
    }
    this.updateProjectsList();
  }

  removeFromSelectedIncludedPipeline(pipeline: string) {
    const indexInclude = this.selectedFilters.includedPipelines.indexOf(pipeline);
    if (indexInclude >= 0) {
      this.selectedFilters.includedPipelines.splice(indexInclude, 1);
      this.localStorageService.setSelectedFilters(this.selectedFilters);
    }
    this.updateProjectsList();
  }

  // ---


  // gets called with every keyup of the searchbar
  processInput() {
    const input = this.searchInputCtrl.getRawValue();
    let lowercaseInput = input ? input.toLowerCase().trim() : '';
    if (lowercaseInput.startsWith(this.TAG_EXCLUDE_PREFIX)) {
      lowercaseInput = lowercaseInput.replace(this.TAG_EXCLUDE_PREFIX, '');
      this._availableExTagsValue = of(this.availableTagsValue
        .filter(value => {
          if (this.selectedFilters.excludedTags.indexOf(value) < 0 && !value.startsWith(this.CONTEXT_PREFIX)) {
            return value.toLowerCase().includes(lowercaseInput);
          }
        })
        .map(value => `-#${value}`)
      );
    } else if (lowercaseInput.startsWith(this.TAG_PREFIX)) {
      lowercaseInput = lowercaseInput.replace(this.TAG_PREFIX, '');
      this._availableInTagsValue = of(this.availableTagsValue
        .filter(value => {
          if (this.selectedFilters.includedTags.indexOf(value) < 0 && !value.startsWith(this.CONTEXT_PREFIX)) {
            return value.toLowerCase().includes(lowercaseInput);
          }
        })
        .map(value => `#${value}`)
      );
    } else if (lowercaseInput.startsWith(this.PIPELINE_PREFIX)) {
      lowercaseInput = lowercaseInput.replace(this.PIPELINE_PREFIX, '');
      this._availablePipelinesValue = of(this.availablePipelinesValue
        .filter(value => {
          if (this.selectedFilters.includedPipelines.indexOf(value) < 0 && !value.startsWith(this.PIPELINE_PREFIX)) {
            return value.toLowerCase().includes(lowercaseInput);
          }
        })
        .map(value => `pipeline:${value}`)
      );
    } else {
      this.filteredProjects = this.getFilteredProjects();
      this.filteredProjects = this.filteredProjects.filter(project => {
        return project.name.toLowerCase().includes(lowercaseInput) || project.name.toLowerCase().startsWith(lowercaseInput);
      })
      this.filteredProjectsOutput.emit(this.filteredProjects);
      this.appRef.tick();
    }
  }

  updateProjectsList() {
    if (this.projectsValue == undefined) {
      this.filteredProjectsOutput.emit(undefined);
      return;
    }
    this.filteredProjects = this.getFilteredProjects();
    console.log(this.filteredProjects);
    this.filteredProjectsOutput.emit(this.filteredProjects);
  }

  getFilteredProjects() {
    return this.projectsValue.filter(project => {
      // handle context view
      if (this.selectedFilters.includedTags.filter(tag => tag.startsWith(this.CONTEXT_PREFIX)).length > 0) {
        const contextFilter = this.selectedFilters.includedTags.filter(tag => tag.startsWith(this.CONTEXT_PREFIX))[0];
        const isInContext = project.tags.some(tag => tag == contextFilter);
        if (!isInContext) return false;
        if (this.selectedFilters.excludedTags.length > 0) {
          const hasExcludedTag = project.tags.some(tag => this.selectedFilters.excludedTags.includes(tag));
          if (hasExcludedTag) {
            return false;
          }
        }
        return true;
      } else {
        // handle [All] projects view (or no context at all)
        if (this.selectedFilters.includedPipelines.length > 0) {
          console.log(project.name);
          console.log(project.pipelineDefinition.name);
          console.log(this.selectedFilters.includedPipelines.includes(project.pipelineDefinition.name));
          const hasIncludedPipeline = this.selectedFilters.includedPipelines.includes(project.pipelineDefinition.name);
          if (!hasIncludedPipeline) {
            return false;
          }
        }
        if (project.tags.length === 0) {
          return true;
        }
        if (this.selectedFilters.includedTags.length > 0) {
          const hasIncludedTag = project.tags.some(tag => this.selectedFilters.includedTags.includes(tag));
          if (!hasIncludedTag) {
            return false;
          }
        }
        if (this.selectedFilters.excludedTags.length > 0) {
          const hasExcludedTag = project.tags.some(tag => this.selectedFilters.excludedTags.includes(tag));
          if (hasExcludedTag) {
            return false;
          }
        }
        return true;
      }
    });
  }

  applyFilterMiddleware(value: string) {
    let input = value.trim() || '';
    console.log(input)
    if (input.startsWith(this.TAG_PREFIX) && (this.selectedFilters.includedTags.indexOf(input) < 0)) {
      input = input.replace(this.TAG_PREFIX, '');
      this.addIncludedTag(input);
    } else if (input.startsWith(this.TAG_EXCLUDE_PREFIX) && (this.selectedFilters.excludedTags.indexOf(input) < 0)) {
      input = input.replace(this.TAG_EXCLUDE_PREFIX, '');
      this.addExcludedTag(input);
    } else if (input.startsWith(this.PIPELINE_PREFIX) && (this.selectedFilters.includedPipelines.indexOf(input) < 0)) {
      input = input.replace(this.PIPELINE_PREFIX, '');
      this.addIncludedPipeline(input);
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
    if (value.startsWith(this.TAG_PREFIX) || value.startsWith(this.TAG_EXCLUDE_PREFIX) || value.startsWith(this.PIPELINE_PREFIX)) {
      if (input) {
        input.value = '';
      }
      this.searchInputCtrl.setValue(null);
    }
  }

  emitGroups() {
    this.projectsGroups.emit(this.projectsGroupsValue);
    this.groupsOnTop.emit(this.groupsOnTopIsChecked);
  }

  // used for dynamic height of the search bar
  isSearchEmpty() {
    return (
      this.selectedFilters.includedTags.filter(tag => !tag.startsWith(this.CONTEXT_PREFIX)).length <= 0 &&
      this.selectedFilters.excludedTags.length <= 0 &&
      this.selectedFilters.includedPipelines.length <= 0
    );
  }
}
