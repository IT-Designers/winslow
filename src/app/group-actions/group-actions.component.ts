import {Component, OnInit} from '@angular/core';
import {ProjectApiService} from '../api/project-api.service';

@Component({
  selector: 'app-group-actions',
  templateUrl: './group-actions.component.html',
  styleUrls: ['./group-actions.component.css']
})
export class GroupActionsComponent implements OnInit {


  constructor(public api: ProjectApiService) {
  }

  ngOnInit() {
  }

}
