import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {TopbarComponent} from './topbar/topbar.component';
import {RouterModule} from '@angular/router';
import {SystemOverviewComponent} from './system-overview/system-overview.component';
import {PipelinesComponent} from './pipelines/pipelines.component';
import {HttpClientModule, HttpClientXsrfModule} from '@angular/common/http';
import {FilesComponent} from './files/files.component';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {MatDialogModule, MatDialogRef} from '@angular/material/dialog';
import {
  MatAutocompleteModule,
  MatButtonModule,
  MatButtonToggleModule,
  MatCardModule,
  MatCheckboxModule,
  MatChipsModule,
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
  MatTableModule,
  MatTabsModule,
  MatTooltipModule
} from '@angular/material';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {DragDropDirectiveDirective} from './drag-drop-directive.directive';
import {ProjectsComponent} from './projects/projects.component';
import {ProjectsCreateDialog} from './projects-create-dialog/projects-create-dialog.component';
import {StateIconComponent} from './state-icon/state-icon.component';
import {ProjectViewComponent,} from './project-view/project-view.component';
import {FileBrowseDialog} from './file-browse-dialog/file-browse-dialog.component';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {ServerComponent} from './server/server.component';
import {ServersComponent} from './servers/servers.component';
import {ServersOverviewComponent} from './servers-overview/servers-overview.component';
import {AboutComponent} from './about/about.component';
import {CreatePipelineDialogComponent} from './pipeline-create-dialog/create-pipeline-dialog.component';
import {LoadingInfoComponent} from './connect-failed-info/loading-info.component';
import {GroupActionsComponent} from './group-actions/group-actions.component';
import {ScrollingModule} from '@angular/cdk/scrolling';
import {TagsWithAutocompleteComponent} from './tags-with-autocomplete/tags-with-autocomplete.component';
import {ProjectViewHeaderComponent} from './project-view-header/project-view-header.component';
import {StageExecutionSelectionComponent} from './stage-execution-selection/stage-execution-selection.component';
import {ProjectListComponent} from './project-list/project-list.component';
import {TagFilterComponent} from './tag-filter/tag-filter.component';
import {GroupSettingsDialogComponent} from './group-settings-dialog/group-settings-dialog.component';
import {SweetAlert2Module} from '@sweetalert2/ngx-sweetalert2';
import {SystemViewComponent} from './system-view/system-view.component';
import { SystemCfgEnvComponent } from './system-cfg-env/system-cfg-env.component';
import { EnvVariablesComponent } from './env-variables/env-variables.component';
import { PipelineEditorComponent } from './pipeline-editor/pipeline-editor.component';
import {MonacoEditorModule} from 'ngx-monaco-editor';
import { ProjectOverviewComponent } from './project-overview/project-overview.component';
import { ProjectHistoryHeaderComponent } from './project-history-header/project-history-header.component';

@NgModule({
  declarations: [
    AppComponent,
    TopbarComponent,
    SystemOverviewComponent,
    PipelinesComponent,
    FilesComponent,
    DragDropDirectiveDirective,
    ProjectsComponent,
    ProjectsCreateDialog,
    StateIconComponent,
    ProjectViewComponent,
    FileBrowseDialog,
    ServerComponent,
    ServersComponent,
    ServersOverviewComponent,
    AboutComponent,
    CreatePipelineDialogComponent,
    LoadingInfoComponent,
    GroupActionsComponent,
    TagsWithAutocompleteComponent,
    ProjectViewHeaderComponent,
    StageExecutionSelectionComponent,
    GroupSettingsDialogComponent,
    ProjectListComponent,
    TagFilterComponent,
    SystemViewComponent,
    SystemCfgEnvComponent,
    EnvVariablesComponent,
    PipelineEditorComponent,
    ProjectOverviewComponent,
    ProjectHistoryHeaderComponent,
  ],
  imports: [
    SweetAlert2Module.forRoot(),
    MonacoEditorModule.forRoot(),
    HttpClientModule,
    HttpClientXsrfModule.withOptions({
      cookieName: 'XSRF-TOKEN',
      headerName: 'X-XSRF-TOKEN'
    }),

    RouterModule.forRoot([
      {path: '', redirectTo: 'projects/', pathMatch: 'full'},
      {path: 'actions', component: GroupActionsComponent},

      {path: 'projects', redirectTo: 'projects/', pathMatch: 'full'},
      {path: 'projects/:id', redirectTo: 'projects/:id/', pathMatch: 'full'},
      {
        path: 'projects',
        children: [{
          path: ':id',
          component: ProjectsComponent,
          children: [{
            path: ':tab',
            component: ProjectViewComponent,
          }]
        }]
      },

      {path: 'pipelines', component: PipelinesComponent},
      {path: 'files', component: FilesComponent},
      {path: 'servers', component: ServersComponent},
      {path: 'about', component: AboutComponent},
      {path: 'system', component: SystemViewComponent},
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
    MatTabsModule,
    MatCheckboxModule,

    NgxChartsModule,
    MatCardModule,
    ScrollingModule,
    MatAutocompleteModule,
    MatChipsModule,

  ],
  providers: [
    {provide: MatDialogRef, useValue: {}},
  ],
  bootstrap: [AppComponent],
  entryComponents: [
    ProjectsCreateDialog,
    FileBrowseDialog,
    CreatePipelineDialogComponent,
    GroupSettingsDialogComponent,
  ]
})
export class AppModule {
}
