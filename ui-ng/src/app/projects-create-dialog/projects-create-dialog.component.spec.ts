import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ProjectsCreateDialog } from './projects-create-dialog.component';

describe('ProjectCreateComponent', () => {
  let component: ProjectsCreateDialog;
  let fixture: ComponentFixture<ProjectsCreateDialog>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ProjectsCreateDialog ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectsCreateDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
