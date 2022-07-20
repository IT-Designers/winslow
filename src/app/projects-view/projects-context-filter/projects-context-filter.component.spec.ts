import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectsContextFilterComponent } from './projects-context-filter.component';

describe('ProjectsContextFilterComponent', () => {
  let component: ProjectsContextFilterComponent;
  let fixture: ComponentFixture<ProjectsContextFilterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectsContextFilterComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectsContextFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
