import {Injectable} from '@angular/core';
import {Raw} from "./winslow-api";

@Injectable({
  providedIn: 'root'
})
export class LocalStorageService {

  constructor() {
  }

  private readonly KEY_CHART_SETTINGS = 'winslow-chart-settings';

  getChartSettings(): GlobalChartSettings {
    const item = localStorage.getItem(this.KEY_CHART_SETTINGS);
    if (item != null) {
      const parsed = JSON.parse(item);
      if (parsed != null) {
        return new GlobalChartSettings(parsed);
      }
    }
    return new GlobalChartSettings({
      enableEntryLimit: false,
      enableRefreshing: false,
      entryLimit: 50,
      refreshTimerInSeconds: 5
    });
  }

  setChartSettings(item: GlobalChartSettings): void {
    localStorage.setItem(this.KEY_CHART_SETTINGS, JSON.stringify(item));
  }

  getSettings(KEY: string) {
    const item = localStorage.getItem(KEY);
    if (item == null) {
      return null;
    }
    return JSON.parse(item);
  }

  setSettings(KEY: string, data: any) {
    localStorage.setItem(KEY, JSON.stringify(data));
  }
}

export class GlobalChartSettings {
  constructor(data: Raw<GlobalChartSettings>) {
    this.enableEntryLimit = data.enableEntryLimit;
    this.entryLimit = data.entryLimit;
    this.enableRefreshing = data.enableRefreshing;
    this.refreshTimerInSeconds = data.refreshTimerInSeconds;
  }

  enableEntryLimit: boolean;
  entryLimit: number;
  enableRefreshing: boolean;
  refreshTimerInSeconds: number;
}
