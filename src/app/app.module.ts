import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppComponent } from './app.component';
import { TopbarComponent } from './topbar/topbar.component';
import {RouterModule} from '@angular/router';
import { SystemOverviewComponent } from './system-overview/system-overview.component';

@NgModule({
  declarations: [
    AppComponent,
    TopbarComponent,
    SystemOverviewComponent
  ],
  imports: [
    BrowserModule,
    RouterModule.forRoot([
      { path: '', component: SystemOverviewComponent},
    ])
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
