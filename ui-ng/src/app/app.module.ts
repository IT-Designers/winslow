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
import {MatLegacyDialogModule as MatDialogModule, MatLegacyDialogRef as MatDialogRef} from '@angular/material/legacy-dialog';
import { MatLegacyAutocompleteModule as MatAutocompleteModule } from '@angular/material/legacy-autocomplete';
import { MatLegacyButtonModule as MatButtonModule } from '@angular/material/legacy-button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatLegacyCardModule as MatCardModule } from '@angular/material/legacy-card';
import { MatLegacyCheckboxModule as MatCheckboxModule } from '@angular/material/legacy-checkbox';
import { MatLegacyChipsModule as MatChipsModule } from '@angular/material/legacy-chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import { MatLegacyInputModule as MatInputModule } from '@angular/material/legacy-input';
import { MatLegacyListModule as MatListModule } from '@angular/material/legacy-list';
import { MatLegacyMenuModule as MatMenuModule } from '@angular/material/legacy-menu';
import { MatLegacyProgressBarModule as MatProgressBarModule } from '@angular/material/legacy-progress-bar';
import { MatLegacyProgressSpinnerModule as MatProgressSpinnerModule } from '@angular/material/legacy-progress-spinner';
import { MatLegacyRadioModule as MatRadioModule } from '@angular/material/legacy-radio';
import { MatLegacySelectModule as MatSelectModule } from '@angular/material/legacy-select';
import { MatLegacySlideToggleModule as MatSlideToggleModule } from '@angular/material/legacy-slide-toggle';
import { MatLegacySnackBarModule as MatSnackBarModule } from '@angular/material/legacy-snack-bar';
import { MatStepperModule } from '@angular/material/stepper';
import { MatLegacyTableModule as MatTableModule } from '@angular/material/legacy-table';
import { MatLegacyTabsModule as MatTabsModule } from '@angular/material/legacy-tabs';
import { MatLegacyTooltipModule as MatTooltipModule } from '@angular/material/legacy-tooltip';
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
import { PipelineViewComponent } from './pipeline-view/pipeline-view.component';
import { DiagramNodeComponent } from './pipeline-view/diagram-node/diagram-node.component';
import { DiagramLibraryComponent } from './pipeline-view/diagram-library/diagram-library.component';
import { EditFormsComponent } from './pipeline-view/diagram-library/edit-forms/edit-forms.component';
import { DiagramGatewayComponent } from './pipeline-view/diagram-gateway/diagram-gateway.component';
import { AddToolsComponent } from './pipeline-view/add-tools/add-tools.component';
import { UserAndGroupManagementComponent } from './user-and-group-management/user-and-group-management.component';
import { GroupMemberListComponent } from './user-and-group-management/group-member-list/group-member-list.component';
import { GroupAddMemberDialogComponent } from './user-and-group-management/group-add-member-dialog/group-add-member-dialog.component';
import { NewGroupDialogComponent } from './user-and-group-management/new-group-dialog/new-group-dialog.component';
import { ProjectGroupsListComponent } from './project-view/project-groups-list/project-groups-list.component';
import { ProjectAddGroupDialogComponent } from './project-view/project-add-group-dialog/project-add-group-dialog.component';
import { SearchableListComponent } from './user-and-group-management/searchable-list/searchable-list.component';
import { UserAddNameDialogComponent } from './user-and-group-management/user-add-name-dialog/user-add-name-dialog.component';
import { UserDetailsComponent } from './user-and-group-management/user-details/user-details.component';
import { GroupDetailsComponent } from './user-and-group-management/group-details/group-details.component';
import { PasswordDialogComponent } from './user-and-group-management/password-dialog/password-dialog.component';
import { GroupAssignmentComponent } from './pipelines/group-assignment/group-assignment.component';
import { RessourcesGroupAssignmentComponent } from './server-details/ressources-group-assignment/ressources-group-assignment.component';
import {MatLegacySliderModule as MatSliderModule} from '@angular/material/legacy-slider';
import { ServerGroupsListComponent } from './server-details/server-groups-list/server-groups-list.component';
import { AddPipelineDialogComponent } from './pipelines/add-pipeline-dialog/add-pipeline-dialog.component';
import { PipelineDetailsComponent } from './pipelines/pipeline-details/pipeline-details.component';
import { RxStompService } from './rx-stomp.service';
import {rxStompServiceFactory} from "./rx-stomp-service-factory";

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
        PipelineViewComponent,
        DiagramNodeComponent,
        DiagramLibraryComponent,
        EditFormsComponent,
        DiagramGatewayComponent,
        AddToolsComponent,
        UserAndGroupManagementComponent,
        GroupMemberListComponent,
        GroupAddMemberDialogComponent,
        NewGroupDialogComponent,
        ProjectGroupsListComponent,
        ProjectAddGroupDialogComponent,
        SearchableListComponent,
        UserAddNameDialogComponent,
        UserDetailsComponent,
        GroupDetailsComponent,
        PasswordDialogComponent,
        GroupAssignmentComponent,
        RessourcesGroupAssignmentComponent,
        ServerGroupsListComponent,
        AddPipelineDialogComponent,
        PipelineDetailsComponent,

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
            { path: '', redirectTo: 'projects/', pathMatch: 'full' },
            { path: 'actions', component: GroupActionsComponent },
            { path: 'projects', redirectTo: 'projects/', pathMatch: 'full' },
            { path: 'projects/:id', redirectTo: 'projects/:id/', pathMatch: 'full' },
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
            { path: 'pipelines', component: PipelinesComponent },
            { path: 'files', component: FilesComponent },
            { path: 'servers', component: ServersComponent },
            { path: 'about', component: AboutComponent },
            { path: 'groups', component: UserAndGroupManagementComponent },
            { path: 'system', redirectTo: 'system/', pathMatch: 'full' },
            {
                path: 'system',
                children: [{
                        path: ':cfg',
                        component: SystemViewComponent
                    }]
            },
        ], {}),
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
        MatSliderModule,
    ],
    providers: [
        {
            provide: MatDialogRef,
            useValue: {}
        },
        {
            provide: RxStompService,
            useFactory: rxStompServiceFactory,
        },
    ],
    bootstrap: [AppComponent]
})
export class AppModule {
}
