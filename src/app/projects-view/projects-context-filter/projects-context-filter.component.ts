import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';

@Component({
  selector: 'app-projects-context-filter',
  templateUrl: './projects-context-filter.component.html',
  styleUrls: ['./projects-context-filter.component.css']
})
export class ProjectsContextFilterComponent implements OnInit {

  availableTagsValue: string[];
  selectedContext: string;
  @Output() outputContext = new EventEmitter<string>();
  CONTEXT_PREFIX = 'context::';
  selectedIndex = 0;

  constructor() {
  }

  ngOnInit(): void {
  }

  @Input()
  set availableTags(tags: string[]) {
    this.availableTagsValue = tags
      .filter(tag => tag.startsWith(this.CONTEXT_PREFIX))
      .map(tag => tag.replace(this.CONTEXT_PREFIX, ''), tag => tag.sort());
  }

  changeContext(selection: string) {
    if (this.selectedContext === selection || selection === '') {
      this.selectedContext = '';
      this.outputContext.emit(undefined);
      this.selectedIndex = 0;
    } else {
      this.selectedContext = selection;
      this.outputContext.emit('context::' + this.selectedContext);
      this.selectedIndex = 1;
    }
  }
}
