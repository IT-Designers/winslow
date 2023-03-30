import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ChartGraph, ChartDisplaySettings, LogChart} from "../log-chart-definition";
import {Subscription} from "rxjs";

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent implements OnInit, OnDestroy {

  options: {}
  subscription: Subscription;

  @Input() chart: LogChart

  constructor() {
  }

  ngOnInit() {
    this.subscription = this.chart.snapshot$.subscribe({
      next: snapshot => {
        this.settings(snapshot.definition.displaySettings)
        this.data(snapshot.graphs)
      }
    })
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe()
  }

  settings(settings: ChartDisplaySettings) {
    const newOptions = {
      title: {
        text: settings.name,
      },
      grid: {
        top: 50,
        bottom: 35,
        left: 40,
        right: 10,
      },
      xAxis: {
        name: settings.xAxisName,
        type: settings.xAxisType,
        min: settings.xAxisMinValue ?? 'dataMin',
        max: settings.xAxisMaxValue ?? 'dataMax',
        nameLocation: 'center',
        nameGap: 25,
      },
      yAxis: {
        name: settings.yAxisName,
        type: settings.yAxisType,
        min: settings.yAxisMinValue ?? 'dataMin',
        max: settings.yAxisMaxValue ?? 'dataMax',
        nameLocation: 'center',
        nameGap: 25,
      },
      tooltip: {
        trigger: 'axis',
      },
      animation: false,
    }
    this.updateOptions(newOptions);
  };

  data(graphs: ChartGraph[]) {
    const series = graphs.map(graph => ({
      type: 'line',
      showSymbol: false,
      data: graph.data,
      name: graph.name,
      color: graph.color,
    }))
    const newOptions = {
      series: series,
    }
    this.updateOptions(newOptions);
  }

  private updateOptions(newOptions) {
    this.options = {...this.options, ...newOptions}
  }
}
