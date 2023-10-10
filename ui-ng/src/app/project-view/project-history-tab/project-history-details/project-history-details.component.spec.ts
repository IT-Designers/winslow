import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectHistoryDetailsComponent } from './project-history-details.component';

describe('ProjectHistoryDetailsComponent', () => {
  let component: ProjectHistoryDetailsComponent;
  let fixture: ComponentFixture<ProjectHistoryDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectHistoryDetailsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectHistoryDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
