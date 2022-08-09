import {Component, Inject, OnInit} from '@angular/core';
import {MAT_DIALOG_DATA} from "@angular/material/dialog";
import {GlobalChartSettings} from "../../api/local-storage.service";

@Component({
  selector: 'app-log-analysis-settings-dialog',
  templateUrl: './log-analysis-settings-dialog.component.html',
  styleUrls: ['./log-analysis-settings-dialog.component.css']
})
export class LogAnalysisSettingsDialogComponent implements OnInit {

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: GlobalChartSettings
  ) {
  }

  ngOnInit(): void {
  }

}
