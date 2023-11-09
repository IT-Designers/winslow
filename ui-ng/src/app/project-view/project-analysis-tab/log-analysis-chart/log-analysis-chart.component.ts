import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ChartAxisType, ChartGraph, LogChart, LogChartDefinition} from "../log-chart-definition";
import {Subscription} from "rxjs";
import {EChartsOption, SeriesOption} from 'echarts'

@Component({
  selector: 'app-log-analysis-chart',
  templateUrl: './log-analysis-chart.component.html',
  styleUrls: ['./log-analysis-chart.component.css']
})
export class LogAnalysisChartComponent implements OnInit, OnDestroy {

  options: EChartsOption = {};
  subscription?: Subscription;

  @Input() chart?: LogChart

  constructor() {
  }

  ngOnInit() {
    if (this.chart == undefined) {
      console.error("Cannot display chart: Input is not initialized.");
      return;
    }
    this.subscription = this.chart.snapshot$.subscribe({
      next: snapshot => {
        this.settings(snapshot.definition)
        this.data(snapshot.graphs)
      }
    })
  }

  ngOnDestroy() {
    this.subscription?.unsubscribe()
  }

  settings(settings: LogChartDefinition) {
    const newOptions: EChartsOption = {
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
        type: this.toEchartsAxisType(settings.xAxisType),
        min: settings.xAxisMinValue ?? 'dataMin',
        max: settings.xAxisMaxValue ?? 'dataMax',
        nameLocation: 'middle',
        nameGap: 25,
      },
      yAxis: {
        name: settings.yAxisName,
        type: this.toEchartsAxisType(settings.yAxisType),
        min: settings.yAxisMinValue ?? 'dataMin',
        max: settings.yAxisMaxValue ?? 'dataMax',
        nameLocation: 'middle',
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
    const series: SeriesOption[] = graphs.map(graph => ({
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

  private updateOptions(newOptions: EChartsOption) {
    this.options = {...this.options, ...newOptions}
  }

  private toEchartsAxisType(original: ChartAxisType) {
    //return original.toLowerCase(); // TypeScript does not like this
    switch (original) {
      case ChartAxisType.VALUE:
        return "value"
      case ChartAxisType.LOG:
        return "log"
      case ChartAxisType.TIME:
        return "time"
    }
  }
}

