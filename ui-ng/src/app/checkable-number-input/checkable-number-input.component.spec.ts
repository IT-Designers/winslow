import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CheckableNumberInputComponent } from './checkable-number-input.component';

describe('CheckableNumberInputComponent', () => {
  let component: CheckableNumberInputComponent;
  let fixture: ComponentFixture<CheckableNumberInputComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ CheckableNumberInputComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(CheckableNumberInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
