import { Component, OnInit } from '@angular/core';
import {LongLoadingDetector} from '../long-loading-detector';

@Component({
  selector: 'app-system-view',
  templateUrl: './system-view.component.html',
  styleUrls: ['./system-view.component.css']
})
export class SystemViewComponent implements OnInit {
  selection?: string;
  longLoading = new LongLoadingDetector();

  constructor() { }

  ngOnInit() {
    this.selection = 'env';
  }

}
