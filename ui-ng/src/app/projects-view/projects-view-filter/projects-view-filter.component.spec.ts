import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectsViewFilterComponent } from './projects-view-filter.component';

describe('ProjectsViewFilterComponent', () => {
  let component: ProjectsViewFilterComponent;
  let fixture: ComponentFixture<ProjectsViewFilterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjectsViewFilterComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ProjectsViewFilterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
