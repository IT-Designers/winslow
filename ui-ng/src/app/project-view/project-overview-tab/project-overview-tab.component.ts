import {ChangeDetectorRef, Component, EventEmitter, Input, OnDestroy, Output} from '@angular/core';
import {ProjectApiService} from '../../api/project-api.service';
import {DialogService} from '../../dialog.service';
import {MatDialog} from '@angular/material/dialog';
import {
  ProjectDiskUsageDialogComponent,
  ProjectDiskUsageDialogData
} from '../../project-disk-usage-dialog/project-disk-usage-dialog.component';
import {Subscription} from 'rxjs';
import {Action, ExecutionGroupInfo, ProjectInfo, StageInfo, State, StatsInfo} from '../../api/winslow-api';


@Component({
  selector: 'app-project-overview-tab',
  templateUrl: './project-overview-tab.component.html',
  styleUrls: ['./project-overview-tab.component.css']
})
export class ProjectOverviewTabComponent implements OnDestroy {


  private static readonly UPDATE_INTERVAL = 1_000;
  private static readonly GRAPH_ENTRIES = 180;

  @Output() clickUseAsBlueprint = new EventEmitter<[ExecutionGroupInfo, StageInfo?]>();
  @Output() clickDeleteEnqueued = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickResumeSingle = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickResume = new EventEmitter<ExecutionGroupInfo>();
  @Output() clickPause = new EventEmitter<ExecutionGroupInfo>();

  nodeName?: string;

  schemeCpu = {domain: ['#DD4444']};
  schemeMemory = {domain: ['#44DD44']};

  stateValue?: State;
  stateFinished?: boolean;
  stateRunning?: boolean;
  statePaused?: boolean;

  lastSuccessfulStatsUpdate = 0;
  seriesInitialized = false;

  mostRecent?: ExecutionGroupInfo;
  projectValue!: ProjectInfo;
  memory: any[] = [];
  memoryMax = 1;
  cpu: any[] = [];
  cpuMax = 100;
  cpuLimit = 0;
  subscription?: Subscription;

  enqueued: ExecutionGroupInfo[] = [];

  mergeOptionCpu = {};
  chartOptionCpu = {
    tooltip: {
      position: 'top',
      confine: true,
      trigger: 'axis',
      axisPointer: {
          type: 'shadow'
      },
      formatter: (params: any) => {
        params = params[0];
        let date = new Date(params.name);
        let zero = (date.getMinutes() < 10 ? "0" : "")
        return date.getDate() + '/' + (date.getMonth() + 1) + '/' + date.getFullYear() + '  ' +
               date.getHours() + ":" + zero + date.getMinutes() + "<br>" +
               params.seriesName + ": " + params.value[1] + " Mhz";
      },
    },
    calculable: false,
    grid: {
      top: "5%",
      bottom: "20",
      left: "70",
      right: "0"
    },
    xAxis: [
      {
        type: "time",
        splitLine: {
          show: false,
        },
        show: true,
        axisLabel: {
          formatter: (value: any) => {
            const date = new Date(value);
            if (date.getSeconds() === 0) {
              let zero = (date.getMinutes() < 10 ? ":0" : ":")
              return date.getHours() + zero + date.getMinutes();
            }
          }
        }
      },
    ],
    yAxis: {
      type: "value",
      axisLabel: {
        formatter: "{value} Mhz",
      },
      scale : true,
      max : 4000,
      min : 0,
      name: "Mhz",

      splitNumber : 5,
      splitLine: {
        show: true,
      },
    },
    series: [],
  };

  mergeOptionMemory = {};
  chartOptionMemory = {
    tooltip: {
      position: 'top',
      confine: true,
      trigger: 'axis',
      axisPointer: {
          type: 'shadow'
      },
      formatter: (params: any) => {
        params = params[0];
        let date = new Date(params.name);
        let zero = (date.getMinutes() < 10 ? "0" : "")
        return date.getDate() + '/' + (date.getMonth() + 1) + '/' + date.getFullYear() + '  ' +
               date.getHours() + ":" + zero + date.getMinutes() + "<br>" +
               params.seriesName + ": " + params.value[1] + " GiB";
      },
    },
    calculable: false,
    grid: {
      top: "5%",
      bottom: "20",
      left: "50",
      right: "0"
    },
    xAxis: [
      {
        type: "time",
        splitLine: {
          show: false,
        },
        show: true,
        axisLabel: {
          formatter: (value: any) => {
            const date = new Date(value);
            if (date.getSeconds() === 0) {
              let zero = (date.getMinutes() < 10 ? ":0" : ":")
              return date.getHours() + zero + date.getMinutes();
            }
          }
        }
      },
    ],
    yAxis: {
      type: "value",
      axisLabel: {
        formatter: function (value: any, index: any) {
          return value.toFixed(0) + ' GiB';
        }
      },
      scale : true,
      max : 8,
      min : 0,
      splitNumber : 5,
      splitLine: {
        show: true,
      },
    },
    series: [],
  };

  constructor(private api: ProjectApiService,
              private dialog: DialogService,
              private createDialog: MatDialog,
              private cdr: ChangeDetectorRef) {
  }

  private static maxOfSeriesOr(series: any[], minimum: number, upperMax: number) {
    let max = minimum;
    for (const entry of series) {
      if (entry.value[1] > max) {
        max = entry.value[1];
      }
    }
    return Math.max(max, Math.min(max * 1.4, upperMax));
  }

  private static limitSeriesTo(container: any, length: number) {
    if (container.series.length >= length) {
      container.series.splice(0, container.series.length - length + 1);
    }
  }


  @Input()
  set project(value: ProjectInfo) {
    this.projectValue = value;
    this.unsubscribe();
    this.subscribe();
    this.enqueued = [];
    this.initSeries();
  }

  @Input()
  set history(history: ExecutionGroupInfo[]) {
    this.mostRecent = undefined;
    this.enqueued = [];
    if (history && history.length > 0) {
      this.mostRecent = history[0];
      for (const entry of history) {
        // because it is ordered, it starts with the enqueued ones
        // and then continues with the old ones
        if (entry.enqueueIndex != null) {
          this.enqueued.push(entry);
        }
      }
    }
  }

  @Input()
  set state(state: State | undefined) {
    this.stateValue = state;
    this.stateFinished = state === 'FAILED' || state === 'SUCCEEDED';
    this.stateRunning = state === 'RUNNING';
    this.statePaused = state === 'PAUSED' || state === 'ENQUEUED';
    this.cdr.detectChanges();
  }

  private initSeries() {
    this.seriesInitialized = true;

    const now = new Date();
    const zeroes = [];

    for (let i = ProjectOverviewTabComponent.GRAPH_ENTRIES; i >= 0; --i) {
      const date = new Date(now.getTime() - (i * ProjectOverviewTabComponent.UPDATE_INTERVAL));
      zeroes.push({
        name: date.toString(),
        value:[date, 0]
      });
    }

    this.memory = [{
      name: 'Used',
      series: zeroes.map(z => z),
    }];
    this.cpu = [{
      name: 'Consumed',
      series: zeroes.map(z => z),
    }];
  }

  ngOnDestroy() {
    this.unsubscribe();
  }

  @Input()
  set visible(value: boolean) {
    if (value && !this.seriesInitialized) {
      this.initSeries();
    }
  }

  onStatsUpdate(stats: StatsInfo) {
    this.updateCpu(stats);
    this.updateMemory(stats);
    this.nodeName = stats.nodeName;
  }

  private updateCpu(stats: StatsInfo) {
    const date = new Date();
    this.cpu[0].series.push({
      name: date.toString(),
      value: [date, stats.cpuUsed.toFixed(0)]/*.toLocaleString('en-US') -- ngx seems to be borked here?*/
    });

    this.cpuMax = ProjectOverviewTabComponent.maxOfSeriesOr(this.cpu[0].series, 100, stats.cpuMaximum);
    this.cpu = this.cpu.map(c => {
      ProjectOverviewTabComponent.limitSeriesTo(c, ProjectOverviewTabComponent.GRAPH_ENTRIES);
      return c;
    });

    if(stats.cpuMaximum != 0) {
      this.cpuLimit = stats.cpuMaximum;
    }

    this.mergeOptionCpu = {
      yAxis: {
        max : this.cpuMax
      },
      series: [
        {
          name: "CPU",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#5ac8fa",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.cpu[0].series,
          markLine: {
            data: [{ yAxis: this.cpuLimit}],
            symbol: "none",
          }
        },
      ],
    };
  }


  private updateMemory(stats: StatsInfo) {
    const date = new Date();
    this.memory[0].series.push({
      name: date.toString(),
      value: [date, this.bytesToGigabyte(stats.memoryAllocated).toFixed(2)]/*.toLocaleString('en-US') -- ngx seems to be borked here?*/
    });

    this.memoryMax = ProjectOverviewTabComponent.maxOfSeriesOr(this.memory[0].series, 0.1, stats.memoryMaximum);
    this.memory = this.memory.map(m => {
      ProjectOverviewTabComponent.limitSeriesTo(m, ProjectOverviewTabComponent.GRAPH_ENTRIES);
      return m;
    });

    this.mergeOptionMemory = {
      yAxis: {
        max : this.memoryMax
      },
      series: [
        {
          name: "Memory",
          type: "line",
          hoverAnimation: false,
          showSymbol: false,
          color: "#5ac8fa",
          itemStyle: { normal: { areaStyle: { type: "default" } } },
          data: this.memory[0].series,
        },
      ],
    };
  }

  bytesToGigabyte(bytes: number) {
    return bytes / (1024 * 1024 * 1024);
  }

  pause() {
    if (this.projectValue) {
      this.dialog.openLoadingIndicator(
        this.api
          .pause(this.projectValue.id)
          .then(r => this.statePaused = true),
        `Pausing pipeline...`
      );
    }
  }

  resume() {
    if (this.projectValue) {
      this.dialog.openLoadingIndicator(
        this.api
          .resume(this.projectValue.id)
          .then(r => this.statePaused = false),
        `Resuming pipeline...`
      );
    }
  }

  openProjectDiskUsageDialog() {
    this.createDialog
      .open(ProjectDiskUsageDialogComponent, {
        data: {
          projects: [this.projectValue],
        } as ProjectDiskUsageDialogData
      });
  }
  private subscribe() {
    this.unsubscribe();
    this.subscription = this.api.watchProjectStats(this.projectValue.id, stats => {
      this.onStatsUpdate(stats);
    });
  }

  private unsubscribe() {
    this.subscription?.unsubscribe();
  }
}
