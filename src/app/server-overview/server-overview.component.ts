import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-server-overview',
  templateUrl: './server-overview.component.html',
  styleUrls: ['./server-overview.component.css']
})
export class ServerOverviewComponent implements OnInit {
  single: any[] = [];
  series: any[] = [];
  memory: any[] = [];


  colorScheme = {
//    domain: ['#FF6666', '#66FF66', '#6666FF', '#777777']
    domain: [
      '#FF0000', '#FF7F00',
      '#FFFF00', '#00FF00',
      '#0000FF', '#0000FF',
      '#4B0082', '#9400D3'
    ]
  };

  constructor() {

  }

  ngOnInit() {
    for (let i = 0; i < 8; ++i) {
      this.single.push({
        'name': 'CPU' + i,
        'value': Math.random() * 100,
      });
    }

    setInterval(() => {
      const count = this.single.length;
      this.single = [];
      for (let i = 0; i < count; ++i) {
        this.single.push({
          'name': 'CPU' + i,
          'value': Math.random() * 100
        });
      }


      this.series = [];
      const series1 = [];
      const series2 = [];
      for (let i = 0; i < 120; ++i) {
        series1.push({
          name: new Date(new Date().getTime()  + i * 10_000),
          value: Math.random() * 128 * 1024,
        });
        series2.push({
          name: new Date(new Date().getTime()  + i * 10000),
          value: Math.random() * 128 * 1024,
        });
      }
      this.series.push({
        'name': 'Tx',
        'series': series1
      });
      this.series.push({
        'name': 'Rx',
        'series': series2
      });



      this.memory = [];
      const mem1 = [];
      const mem2 = [];
      for (let i = 0; i < 120; ++i) {
        mem1.push({
          name: new Date(new Date().getTime()  + i * 10_000),
          value: 4 * 1024 * 1024 + Math.random() * 124 * 1024,
        });
        mem2.push({
          name: new Date(new Date().getTime()  + i * 10000),
          value: 1024 * 1024 + Math.random() * 1024 * 1024,
        });
      }
      this.memory.push({
        'name': 'Heap',
        'series': mem1
      });
      this.memory.push({
        'name': 'SWAP',
        'series': mem2
      });
    }, 1000);
  }
}
