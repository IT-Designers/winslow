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
import {
  MatButtonModule,
  MatButtonToggleModule,
  MatExpansionModule,
  MatGridListModule,
  MatIconModule,
  MatInputModule,
  MatListModule,
  MatProgressBarModule,
  MatProgressSpinnerModule,
  MatSelectModule,
  MatSlideToggleModule,
  MatSnackBarModule,
  MatStepperModule,
  MatTableModule, MatTabsModule,
  MatTooltipModule
} from '@angular/material';
import {FormBuilder, FormsModule, ReactiveFormsModule} from '@angular/forms';
import { DragDropDirectiveDirective } from './drag-drop-directive.directive';
import { ProjectsComponent } from './projects/projects.component';
import { ProjectsCreateDialog } from './projects-create/projects-create-dialog.component';
import { StateIconComponent } from './state-icon/state-icon.component';
import { ProjectViewComponent } from './project-view/project-view.component';

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
    DragDropDirectiveDirective,
    ProjectsComponent,
    ProjectsCreateDialog,
    StateIconComponent,
    ProjectViewComponent
  ],
  imports: [
    HttpClientModule,
    HttpClientXsrfModule.withOptions({
      cookieName: 'XSRF-TOKEN',
      headerName: 'X-XSRF-TOKEN'
    }),

    RouterModule.forRoot([
      {path: '', component: SystemOverviewComponent},
      {path: 'pipelines', component: PipelinesComponent},
      {path: 'files', component: FilesComponent},
      {path: 'projects', component: ProjectsComponent}
    ]),

    BrowserModule,
    BrowserAnimationsModule,

    FormsModule,
    ReactiveFormsModule,

    MatDialogModule,
    MatInputModule,
    MatButtonModule,
    MatListModule,
    MatProgressBarModule,
    MatButtonToggleModule,
    MatExpansionModule,
    MatStepperModule,
    MatSelectModule,
    MatTableModule,
    MatGridListModule,
    MatSnackBarModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatTabsModule

  ],
  providers: [
    {provide: MatDialogRef, useValue: {}},
  ],
  bootstrap: [AppComponent],
  entryComponents: [
    CreateDirectoryDialog,
    UploadFilesProgressDialog,
    DeleteAreYouSureDialog,
    ProjectsCreateDialog
  ]
})
export class AppModule { }
