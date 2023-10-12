import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjectSettingsTabComponent } from './project-settings-tab.component';

describe('ProjectSettingsTabComponent', () => {
  let component: ProjectSettingsTabComponent;
  let fixture: ComponentFixture<ProjectSettingsTabComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProjectSettingsTabComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProjectSettingsTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
