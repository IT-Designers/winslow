import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PipelineHeadSettingsComponent } from './pipeline-head-settings.component';

describe('PipelineHeadSettingsComponent', () => {
  let component: PipelineHeadSettingsComponent;
  let fixture: ComponentFixture<PipelineHeadSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PipelineHeadSettingsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(PipelineHeadSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
