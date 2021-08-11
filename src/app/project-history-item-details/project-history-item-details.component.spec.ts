import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectHistoryItemDetailsComponent } from './project-history-item-details.component';

describe('ProjectHistoryItemDetailsComponent', () => {
  let component: ProjectHistoryItemDetailsComponent;
  let fixture: ComponentFixture<ProjectHistoryItemDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectHistoryItemDetailsComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectHistoryItemDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
