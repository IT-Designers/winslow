import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { EnvVariablesComponent } from './env-variables.component';

describe('EnvVariablesComponent', () => {
  let component: EnvVariablesComponent;
  let fixture: ComponentFixture<EnvVariablesComponent>;

  beforeEach(async(() => {
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
