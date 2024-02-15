import {Injectable} from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LocalStorageService {

  constructor() {
  }

  private readonly KEY_CHART_SETTINGS = 'winslow-chart-settings';
  private readonly KEY_PROJECT_FILTERS = 'winslow-projects-filters';
  private readonly KEY_SELECTED_CONTEXT = 'winslow-selected-context';
  private readonly KEY_GROUPS_ON_TOP = 'winslow-groups-on-top';
  private readonly KEY_GROUPS_ACTIVATED = "winslow-groups-activated";

  private get<T>(key: string): T | null {
    const item = localStorage.getItem(key);
    if (item == null) {
      return null;
    }
    return JSON.parse(item);
  }

  private set<T>(key: string, data: T | null): void {
    if (data == null) {
      localStorage.removeItem(key);
    } else {
      localStorage.setItem(key, JSON.stringify(data));
    }
  }

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

  getSelectedContext(): string | null {
    return this.get<string>(this.KEY_SELECTED_CONTEXT);
  }

  setSelectedContext(data: string | null): void {
    return this.set<string>(this.KEY_SELECTED_CONTEXT, data);
  }

  getGroupsOnTop(): boolean | null {
    return this.get<boolean>(this.KEY_GROUPS_ON_TOP);
  }

  setGroupsOnTop(data: boolean | null): void {
    return this.set<boolean>(this.KEY_GROUPS_ON_TOP, data);
  }

  getGroupsActivated(): boolean | null {
    return this.get<boolean>(this.KEY_GROUPS_ACTIVATED);
  }

  setGroupsActivated(data: boolean | null): void {
    return this.set<boolean>(this.KEY_GROUPS_ACTIVATED, data);
  }

  getSelectedFilters(): SelectedTags {
    const item = localStorage.getItem(this.KEY_PROJECT_FILTERS);
    if (item != null) {
      const parsed = JSON.parse(item);
      if (parsed != null) {
        return new SelectedTags(parsed);
      }
    }
    return new SelectedTags({
      includedTags: [],
      excludedTags: [],
      includedPipelines: [],
      excludedPipelines: [],
      includedStates: [],
      excludedStates: [],
    });
  }

  setSelectedFilters(item: SelectedTags): void {
    localStorage.setItem(this.KEY_PROJECT_FILTERS, JSON.stringify(item));
  }
}

export class GlobalChartSettings {
  constructor(data: GlobalChartSettings) {
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

export class SelectedTags {
  constructor(data: SelectedTags) {
    this.includedTags = data.includedTags;
    this.excludedTags = data.excludedTags;
    this.includedPipelines = data.includedPipelines;
    this.excludedPipelines = data.excludedPipelines;
    this.includedStates = data.includedStates;
    this.excludedStates = data.excludedStates;
  }
  includedTags: string[] = []
  excludedTags: string[] = []
  includedPipelines: string[] = []
  excludedPipelines: string[] = []
  includedStates: string[] = []
  excludedStates: string[] = []
}
