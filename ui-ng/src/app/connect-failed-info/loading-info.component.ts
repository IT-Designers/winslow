import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-loading-info',
  templateUrl: './loading-info.component.html',
  styleUrls: ['./connect-failed-info.css']
})
export class LoadingInfoComponent implements OnInit {
  @Input() loading?: boolean;
  @Input() error?: string;

  constructor() { }

  ngOnInit() {
  }

}
