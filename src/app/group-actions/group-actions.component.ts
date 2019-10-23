import {Component, OnInit} from '@angular/core';
import {Project, ProjectApiService} from '../api/project-api.service';
import {LongLoadingDetector} from '../long-loading-detector';

@Component({
  selector: 'app-group-actions',
  templateUrl: './group-actions.component.html',
  styleUrls: ['./group-actions.component.css']
})
export class GroupActionsComponent implements OnInit {

  loadError = null;
  longLoading = new LongLoadingDetector();
  projects: Project[] = null;
  

  constructor(public api: ProjectApiService) {
  }

  ngOnInit() {
  }

}
