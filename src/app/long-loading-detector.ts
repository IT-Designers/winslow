export class LongLoadingDetector {
  loadingCount: number = null;
  loadingSince: number = null;
  longLoadingMs: number = null;

  constructor(msForCategorizationAsLongLoading = 100) {
    this.loadingCount = 0;
    this.longLoadingMs = msForCategorizationAsLongLoading;
  }

  increase() {
    this.loadingCount += 1;
    if (this.loadingSince == null) {
      this.loadingSince = new Date().getTime();
    }
  }

  decrease() {
    this.loadingCount -= 1;
    if (this.loadingCount <= 0) {
      this.loadingSince = null;
    }
  }

  isLongLoading() {
    return this.loadingSince != null && this.loadingSince + this.longLoadingMs <= new Date().getTime();
  }
}
