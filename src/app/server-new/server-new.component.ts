import {Component, Input, OnInit} from '@angular/core';
import {GpuInfo, NodeInfo, NodesApiService} from '../api/nodes-api.service';
import { EChartOption } from 'echarts';

@Component({
  selector: 'app-server-new',
  templateUrl: './server-new.component.html',
  styleUrls: ['./server-new.component.css']
})
export class ServerNewComponent implements OnInit {

  constructor() { }

  @Input('node') node: NodeInfo;

  ngOnInit(): void {
  }



}
