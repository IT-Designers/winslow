import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectsCreateDialog } from './projects-create-dialog.component';

describe('ProjectCreateComponent', () => {
  let component: ProjectsCreateDialog;
  let fixture: ComponentFixture<ProjectsCreateDialog>;

  beforeEach(async(() => {
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
