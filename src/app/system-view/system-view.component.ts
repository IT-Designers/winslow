import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {LongLoadingDetector} from '../long-loading-detector';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';

@Component({
  selector: 'app-system-view',
  templateUrl: './system-view.component.html',
  styleUrls: ['./system-view.component.css']
})
export class SystemViewComponent implements OnInit, OnDestroy {
  selection?: string;
  longLoading = new LongLoadingDetector();

  routeSubscription: Subscription;

  constructor(private route: ActivatedRoute,
              private router: Router) {
    this.routeSubscription = route.params.subscribe(params => {
      if (params.cfg != null) {
        this.selection = params.cfg;
      } else {
        this.selection = 'env';
      }
    });
  }

  ngOnInit() {
    if (this.selection == null) {
      this.selection = 'env';
    }
  }

  ngOnDestroy() {
    this.routeSubscription.unsubscribe();
  }

  public setSelection(selection: string) {
    this.selection = selection;
    this.router.navigate([selection], {
      relativeTo: this.route.parent
    });
  }

}
