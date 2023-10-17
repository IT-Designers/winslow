import { ComponentFixture, TestBed } from '@angular/core/testing';

<<<<<<<< HEAD:ui-ng/src/app/project-view/project-control-tab/project-control-tab.component.spec.ts
import { ProjectControlTabComponent } from './project-control-tab.component';

describe('ProjectControlTabComponent', () => {
  let component: ProjectControlTabComponent;
  let fixture: ComponentFixture<ProjectControlTabComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectControlTabComponent ]
========
import { ProjectLogsTabComponent } from './project-logs-tab.component';

describe('ProjectLogsTabComponent', () => {
  let component: ProjectLogsTabComponent;
  let fixture: ComponentFixture<ProjectLogsTabComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectLogsTabComponent ]
>>>>>>>> 00bbea6b (Resolve "Only use global Pipeline-Definitions instead of using the global and local ones"):ui-ng/src/app/project-view/project-logs-tab/project-logs-tab.component.spec.ts
    })
    .compileComponents();
  });

  beforeEach(() => {
<<<<<<<< HEAD:ui-ng/src/app/project-view/project-control-tab/project-control-tab.component.spec.ts
    fixture = TestBed.createComponent(ProjectControlTabComponent);
========
    fixture = TestBed.createComponent(ProjectLogsTabComponent);
>>>>>>>> 00bbea6b (Resolve "Only use global Pipeline-Definitions instead of using the global and local ones"):ui-ng/src/app/project-view/project-logs-tab/project-logs-tab.component.spec.ts
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
