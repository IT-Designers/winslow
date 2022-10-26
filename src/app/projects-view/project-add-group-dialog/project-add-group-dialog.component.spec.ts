import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectAddGroupDialogComponent } from './project-add-group-dialog.component';

describe('ProjectAddGroupDialogComponent', () => {
  let component: ProjectAddGroupDialogComponent;
  let fixture: ComponentFixture<ProjectAddGroupDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectAddGroupDialogComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectAddGroupDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
