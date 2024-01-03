import { ComponentFixture, TestBed } from '@angular/core/testing';

import { WorkerSettingsComponent } from './worker-settings.component';

describe('WorkerSettingsComponent', () => {
  let component: WorkerSettingsComponent;
  let fixture: ComponentFixture<WorkerSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WorkerSettingsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(WorkerSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
