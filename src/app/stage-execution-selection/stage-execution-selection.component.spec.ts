import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { StageExecutionSelectionComponent } from './stage-execution-selection.component';

describe('StageExecutionSelectionComponent', () => {
  let component: StageExecutionSelectionComponent;
  let fixture: ComponentFixture<StageExecutionSelectionComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ StageExecutionSelectionComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StageExecutionSelectionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
