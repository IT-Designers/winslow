import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { StateIconComponent } from './state-icon.component';

describe('StateIconComponent', () => {
  let component: StateIconComponent;
  let fixture: ComponentFixture<StateIconComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ StateIconComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(StateIconComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
