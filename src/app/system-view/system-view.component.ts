import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-system-view',
  templateUrl: './system-view.component.html',
  styleUrls: ['./system-view.component.css']
})
export class SystemViewComponent implements OnInit {
  selection?: string;

  constructor() { }

  ngOnInit() {
    this.selection = 'env';
  }

}
