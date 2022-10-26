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
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {DragDropDirectiveDirective} from './drag-drop-directive.directive';
import {ProjectsComponent} from './projects/projects.component';
import {ProjectsCreateDialog} from './projects-create-dialog/projects-create-dialog.component';
import {StateIconComponent} from './state-icon/state-icon.component';
import {ProjectViewComponent } from './project-view/project-view.component';
import {FileBrowseDialog} from './file-browse-dialog/file-browse-dialog.component';
import {NgxChartsModule} from '@swimlane/ngx-charts';
import {AboutComponent} from './about/about.component';
import {CreatePipelineDialogComponent} from './pipeline-create-dialog/create-pipeline-dialog.component';
import {LoadingInfoComponent} from './connect-failed-info/loading-info.component';
import {GroupActionsComponent} from './group-actions/group-actions.component';
import {ScrollingModule} from '@angular/cdk/scrolling';
import {TagsWithAutocompleteComponent} from './projects-view/tags-with-autocomplete/tags-with-autocomplete.component';
import {ProjectViewHeaderComponent} from './projects-view/project-view-header/project-view-header.component';
import {StageExecutionSelectionComponent} from './stage-execution-selection/stage-execution-selection.component';
import {ProjectListComponent} from './project-list/project-list.component';
import {TagFilterComponent} from './projects-view/tag-filter/tag-filter.component';
import {GroupSettingsDialogComponent} from './group-settings-dialog/group-settings-dialog.component';
import {SweetAlert2Module} from '@sweetalert2/ngx-sweetalert2';
import {SystemViewComponent} from './system-view/system-view.component';
import { SystemCfgEnvComponent } from './system-cfg-env/system-cfg-env.component';
import { EnvVariablesComponent } from './env-variables/env-variables.component';
import { PipelineEditorComponent } from './pipeline-editor/pipeline-editor.component';
import {MonacoEditorModule} from 'ngx-monaco-editor';
import { ProjectOverviewComponent } from './project-overview/project-overview.component';
import { ProjectHistoryHeaderComponent } from './project-history-header/project-history-header.component';
import { ProjectDiskUsageDialogComponent } from './project-disk-usage-dialog/project-disk-usage-dialog.component';
import { ProjectHistoryComponent } from './project-history/project-history.component';
import {ProjectHistoryGroupInfoComponent} from './project-history-group-info/project-history-group-info.component';
import { InjectableRxStompConfig, RxStompService, rxStompServiceFactory } from '@stomp/ng2-stompjs';
import {RxStompConfig} from './rx-stomp.config';
import { LogViewComponent } from './log-view/log-view.component';
import { StopButtonComponent } from './stop-button/stop-button.component';
import { SystemCfgResLimitComponent } from './system-cfg-res-limit/system-cfg-res-limit.component';
import { CheckableNumberInputComponent } from './checkable-number-input/checkable-number-input.component';
import { ResourceLimitationComponent } from './resource-limitation/resource-limitation.component';
import { ServersComponent } from './servers/servers.component';
import { NgxEchartsModule } from 'ngx-echarts';
import * as echarts from 'echarts';
import { ServerBarComponent } from './server-bar/server-bar.component';
import { ServerDetailsComponent } from './server-details/server-details.component';
import { ProjectHistoryDetailsComponent } from './project-history-details/project-history-details.component';
import { AuthTokensComponent } from './auth-tokens/auth-tokens.component';
import { ProjectsGroupBuilderComponent } from './projects-view/tag-filter/projects-group-builder/projects-group-builder.component';
import { ProjectsViewComponent } from './projects-view/projects-view.component';
import { LogAnalysisComponent } from './log-analysis/log-analysis.component';
import { LogAnalysisChartDialogComponent } from './log-analysis/log-analysis-chart-dialog/log-analysis-chart-dialog.component';
import { LogAnalysisChartComponent } from './log-analysis/log-analysis-chart/log-analysis-chart.component';
import { RegularExpressionVisualiserComponent } from './regular-expression-visualiser/regular-expression-visualiser.component';
import { LogAnalysisSettingsDialogComponent } from './log-analysis/log-analysis-settings-dialog/log-analysis-settings-dialog.component';
import { ProjectsGroupComponent } from './projects-view/projects-group/projects-group.component';
import { ProjectsContextFilterComponent } from './projects-view/tag-filter/projects-context-filter/projects-context-filter.component';
import { AddToContextPopupComponent } from './projects-view/add-to-context-popup/add-to-context-popup.component';
import { RegularExpressionEditorDialogComponent } from './regular-expression-editor-dialog/regular-expression-editor-dialog.component';
import { GroupsViewComponent } from './groups-view/groups-view.component';
import { GroupMemberListComponent } from './group-member-list/group-member-list.component';
import { GroupsAddMemberDialogComponent } from './groups-add-member-dialog/groups-add-member-dialog.component';
import { DeleteConfirmDialogComponent } from './delete-confirm-dialog/delete-confirm-dialog.component';
import { GroupAddNameDialogComponent } from './group-add-name-dialog/group-add-name-dialog.component';
import { GroupTagsWithAutocompleteComponent } from './projects-view/group-tags-with-autocomplete/group-tags-with-autocomplete.component';
import { ProjectGroupsListComponent } from './projects-view/project-groups-list/project-groups-list.component';
import { ProjectAddGroupDialogComponent } from './projects-view/project-add-group-dialog/project-add-group-dialog.component';

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
        ProjectDiskUsageDialogComponent,
        ProjectHistoryComponent,
        ProjectHistoryGroupInfoComponent,
        LogViewComponent,
        StopButtonComponent,
        SystemCfgResLimitComponent,
        CheckableNumberInputComponent,
        ResourceLimitationComponent,
        ServersComponent,
        ServerBarComponent,
        ServerDetailsComponent,
        ProjectHistoryDetailsComponent,
        AuthTokensComponent,
        ProjectsGroupBuilderComponent,
        ProjectsViewComponent,
        LogAnalysisComponent,
        LogAnalysisChartDialogComponent,
        LogAnalysisChartComponent,
        RegularExpressionVisualiserComponent,
        LogAnalysisSettingsDialogComponent,
        ProjectsGroupComponent,
        ProjectsContextFilterComponent,
        AddToContextPopupComponent,
        RegularExpressionEditorDialogComponent,
        GroupsViewComponent,
        GroupMemberListComponent,
        GroupsAddMemberDialogComponent,
        DeleteConfirmDialogComponent,
        GroupAddNameDialogComponent,
        GroupTagsWithAutocompleteComponent,
        ProjectGroupsListComponent,
        ProjectAddGroupDialogComponent,
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
            {path: 'groups', component: GroupsViewComponent},
            {path: 'system', redirectTo: 'system/', pathMatch: 'full'},
            {
              path: 'system',
              children: [{
                path: ':cfg',
                component: SystemViewComponent
              }]
            },
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

        MatToolbarModule,
        MatSidenavModule,
        MatListModule,
        MatButtonModule,
        MatIconModule,

        NgxChartsModule,
        MatCardModule,
        ScrollingModule,
        MatAutocompleteModule,
        MatChipsModule,
        MatMenuModule,
        MatRadioModule,
        NgxEchartsModule.forRoot({
          echarts: { init: echarts.init }
        }),

    ],
  providers: [
    {
      provide: MatDialogRef,
      useValue: {}
    },
    {
      provide: InjectableRxStompConfig,
      useValue: RxStompConfig
    },
    {
      provide: RxStompService,
      useFactory: rxStompServiceFactory,
      deps: [InjectableRxStompConfig]
    }
  ],
  bootstrap: [AppComponent],
  entryComponents: [
    ProjectsCreateDialog,
    ProjectDiskUsageDialogComponent,
    FileBrowseDialog,
    CreatePipelineDialogComponent,
    GroupSettingsDialogComponent,
  ]
})
export class AppModule {
}
