import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { EnvVariablesComponent } from './env-variables.component';

describe('EnvVariablesComponent', () => {
  let component: EnvVariablesComponent;
  let fixture: ComponentFixture<EnvVariablesComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ EnvVariablesComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EnvVariablesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
