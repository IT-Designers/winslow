import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LocalStorageService {

  constructor() {
  }

  private readonly KEY_CHART_SETTINGS = "winslow-chart-settings"

  getChartSettings() {
    const item = JSON.parse(localStorage.getItem(this.KEY_CHART_SETTINGS))
    return item ?? new GlobalChartSettings()
  }

  setChartSettings(item: GlobalChartSettings) {
    localStorage.setItem(this.KEY_CHART_SETTINGS, JSON.stringify(item))
  }
}

export class GlobalChartSettings {
  constructor() {
    this.enableEntryLimit = false
    this.entryLimit = 50
    this.enableRefreshing = true
    this.refreshTimerInSeconds = 5
  }

  enableEntryLimit: boolean
  entryLimit: number
  enableRefreshing: boolean
  refreshTimerInSeconds: number
}
