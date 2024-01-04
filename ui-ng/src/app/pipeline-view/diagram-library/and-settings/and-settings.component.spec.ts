import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AndSettingsComponent } from './and-settings.component';

describe('AndSettingsComponent', () => {
  let component: AndSettingsComponent;
  let fixture: ComponentFixture<AndSettingsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AndSettingsComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(AndSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
