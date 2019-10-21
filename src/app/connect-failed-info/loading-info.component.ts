import {Component, Input, OnInit} from '@angular/core';

@Component({
  selector: 'app-loading-info',
  templateUrl: './loading-info.component.html',
  styleUrls: ['./connect-failed-info.css']
})
export class LoadingInfoComponent implements OnInit {
  @Input() loading: boolean = null;
  @Input() error: string = null;

  constructor() { }

  ngOnInit() {
  }

}
