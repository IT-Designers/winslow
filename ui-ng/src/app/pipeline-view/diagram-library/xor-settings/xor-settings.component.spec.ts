import { ComponentFixture, TestBed } from '@angular/core/testing';

import { XorSettingsComponent } from './xor-settings.component';

describe('XorSettingsComponent', () => {
  let component: XorSettingsComponent;
  let fixture: ComponentFixture<XorSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [XorSettingsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(XorSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
