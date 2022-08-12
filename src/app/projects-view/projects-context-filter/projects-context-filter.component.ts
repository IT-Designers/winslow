import {
  AfterViewInit,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
} from '@angular/core';
import {LocalStorageService} from '../../api/local-storage.service';

@Component({
  selector: 'app-projects-context-filter',
  templateUrl: './projects-context-filter.component.html',
  styleUrls: ['./projects-context-filter.component.css']
})
export class ProjectsContextFilterComponent implements OnInit, AfterViewInit {

  availableTagsValue: string[];
  notVisibleTags = [];
  selectedContext: string;
  SELECTED_CONTEXT = 'SELECTED_CONTEXT';
  @Output() outputContext = new EventEmitter<string>();
  CONTEXT_PREFIX = 'context::';
  selectedIndex = 0;
  observer: IntersectionObserver;

  constructor(private localStorageService: LocalStorageService) {
  }

  ngOnInit(): void {
  }

  ngAfterViewInit() {
    if (this.localStorageService.getSettings(this.SELECTED_CONTEXT) !== '' && this.localStorageService.getSettings(this.SELECTED_CONTEXT) != null) {
      this.changeContext(this.localStorageService.getSettings(this.SELECTED_CONTEXT));
    }
    document.querySelectorAll('.custom-tab').forEach(tab => this.observer.observe(tab));
    this.createTagObserver();
  }

  @Input()
  set availableTags(tags: string[]) {
    this.availableTagsValue = tags
      .filter(tag => tag.startsWith(this.CONTEXT_PREFIX))
      .map(tag => tag.replace(this.CONTEXT_PREFIX, ''), tag => tag.sort());
    document.querySelectorAll('.custom-tab').forEach(tab => this.observer.observe(tab));
  }

  changeContext(selection: string) {
    if (this.selectedContext === selection || selection === '') {
      this.selectedContext = '';
      this.localStorageService.setSettings(this.SELECTED_CONTEXT, selection);
      this.outputContext.emit(undefined);
      this.selectedIndex = 0;
    } else {
      this.selectedContext = selection;
      this.localStorageService.setSettings(this.SELECTED_CONTEXT, selection);
      this.outputContext.emit('context::' + this.selectedContext);
      this.selectedIndex = 1;
    }
    document.querySelectorAll('.custom-tab').forEach(tab => this.observer.observe(tab));
  }

  createTagObserver() {
    this.observer = new IntersectionObserver(entries => {
      for (const entry of entries) {
        const tag = entry.target.textContent;
        if (entry.isIntersecting === true || tag === this.selectedContext) {
          this.notVisibleTags = this.notVisibleTags.filter(name => name !== tag);
        } else {
          if (tag !== this.selectedContext) {
            this.notVisibleTags.push(tag);
          }
        }
      }
      this.notVisibleTags = this.notVisibleTags.sort();
    }, {threshold: [0.8]}); // percent how much an element should visible. 1 if the element must be completely visible
  }
}
