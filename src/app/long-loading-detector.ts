export class LongLoadingDetector {
  loadingCount: number = null;
  loadingSince: number = null;
  longLoadingMs: number = null;

  longLoading = false;

  constructor(msForCategorizationAsLongLoading = 250) {
    this.loadingCount = 0;
    this.longLoadingMs = msForCategorizationAsLongLoading;
  }

  increase() {
    this.loadingCount += 1;
    if (this.loadingSince == null) {
      this.loadingSince = new Date().getTime();
      setTimeout(() => this.checkLongLoading(), this.longLoadingMs + 5);
    }
  }

  decrease() {
    this.loadingCount -= 1;
    if (this.loadingCount <= 0) {
      this.loadingSince = null;
      this.checkLongLoading();
    }
  }

  checkLongLoading() {
    this.longLoading = this.loadingSince != null && this.loadingSince + this.longLoadingMs <= new Date().getTime();
  }

  isLongLoading() {
    return this.longLoading;
  }
}
