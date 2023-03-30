export class LongLoadingDetector {
  loadingCount: number = null;
  loadingSince: number = null;
  longLoadingMs: number = null;

  longLoading = false;

  flags: Set<string> = new Set();

  constructor(msForCategorizationAsLongLoading = 1_000) {
    this.loadingCount = 0;
    this.longLoadingMs = msForCategorizationAsLongLoading;
  }

  increase() {
    this.loadingCount += 1;
    this.increaseCheck();
  }

  private increaseCheck() {
    if (this.loadingSince == null) {
      this.loadingSince = new Date().getTime();
      setTimeout(() => this.checkLongLoading(), this.longLoadingMs + 5);
    }
  }

  decrease() {
    this.loadingCount -= 1;
    this.decreaseCheck();
  }

  private decreaseCheck() {
    if (this.loadingCount + this.flags.size <= 0) {
      this.loadingSince = null;
      this.checkLongLoading();
    }
  }

  raise(flag: string) {
    if (!this.flags.has(flag)) {
      this.flags.add(flag);
      this.increaseCheck();
    }
  }

  clear(flag: string) {
    this.flags.delete(flag);
    this.decreaseCheck();
  }

  checkLongLoading() {
    this.longLoading = this.loadingSince != null && this.loadingSince + this.longLoadingMs <= new Date().getTime();
  }

  isLongLoading() {
    return this.longLoading;
  }
}
