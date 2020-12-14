import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SystemCfgResLimitComponent } from './system-cfg-res-limit.component';

describe('SystemCfgResLimitComponent', () => {
  let component: SystemCfgResLimitComponent;
  let fixture: ComponentFixture<SystemCfgResLimitComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ SystemCfgResLimitComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SystemCfgResLimitComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
