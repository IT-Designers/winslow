import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {TopbarComponent} from './topbar/topbar.component';
import {RouterModule} from '@angular/router';
import {SystemOverviewComponent} from './system-overview/system-overview.component';
import {PipelinesComponent} from './pipelines/pipelines.component';
import {HttpClientModule, HttpClientXsrfModule} from '@angular/common/http';
import {CreateDirectoryDialog, DeleteAreYouSureDialog, FilesComponent, UploadFilesProgressDialog} from './files/files.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatDialogContent, MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {MatButtonModule, MatInputModule, MatListModule, MatProgressBarModule} from '@angular/material';
import {FormsModule} from '@angular/forms';
import { DragDropDirectiveDirective } from './drag-drop-directive.directive';

@NgModule({
  declarations: [
    AppComponent,
    TopbarComponent,
    SystemOverviewComponent,
    PipelinesComponent,
    CreateDirectoryDialog,
    UploadFilesProgressDialog,
    DeleteAreYouSureDialog,
    FilesComponent,
    DragDropDirectiveDirective
  ],
  imports: [
    HttpClientModule,
    HttpClientXsrfModule.withOptions({
      cookieName: 'XSRF-TOKEN',
      headerName: 'X-XSRF-TOKEN'
    }),

    BrowserModule,
    RouterModule.forRoot([
      {path: '', component: SystemOverviewComponent},
      {path: 'pipelines', component: PipelinesComponent},
      {path: 'files', component: FilesComponent}
    ]),

    BrowserAnimationsModule,

    MatDialogModule,
    MatInputModule,
    FormsModule,
    MatButtonModule,
    MatListModule,
    MatProgressBarModule,

  ],
  providers: [
    {provide: MatDialogRef, useValue: {}},
  ],
  bootstrap: [AppComponent],
  entryComponents: [
    CreateDirectoryDialog,
    UploadFilesProgressDialog,
    DeleteAreYouSureDialog
  ]
})
export class AppModule { }
