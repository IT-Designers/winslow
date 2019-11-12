import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SystemCfgEnvComponent } from './system-cfg-env.component';

describe('SystemCfgEnvComponent', () => {
  let component: SystemCfgEnvComponent;
  let fixture: ComponentFixture<SystemCfgEnvComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SystemCfgEnvComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SystemCfgEnvComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
