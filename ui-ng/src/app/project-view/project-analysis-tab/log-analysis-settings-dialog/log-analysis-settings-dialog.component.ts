import {Component, Inject, OnInit} from '@angular/core';
import {CsvFilesService} from '../csv-files.service';
import {GlobalChartSettings, LocalStorageService} from '../../../api/local-storage.service';

@Component({
  selector: 'app-log-analysis-settings-dialog',
  templateUrl: './log-analysis-settings-dialog.component.html',
  styleUrls: ['./log-analysis-settings-dialog.component.css']
})
export class LogAnalysisSettingsDialogComponent implements OnInit {

  settings!: GlobalChartSettings

  constructor(
    private csvFilesService: CsvFilesService,
    private localStorageService: LocalStorageService,
  ) {
  }

  ngOnInit(): void {
    this.settings = this.csvFilesService.globalChartSettings$.getValue();
  }

  save() {
    this.csvFilesService.globalChartSettings$.next(this.settings);
    this.localStorageService.setChartSettings(this.settings);
  }

}
