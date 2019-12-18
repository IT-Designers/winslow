import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectDiskUsageDialogComponent } from './project-disk-usage-dialog.component';

describe('ProjectDiskUsageDialogComponent', () => {
  let component: ProjectDiskUsageDialogComponent;
  let fixture: ComponentFixture<ProjectDiskUsageDialogComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ProjectDiskUsageDialogComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectDiskUsageDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
