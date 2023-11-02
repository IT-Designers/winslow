import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ControlViewLibraryComponent } from './control-view-library.component';

describe('ControlViewLibraryComponent', () => {
  let component: ControlViewLibraryComponent;
  let fixture: ComponentFixture<ControlViewLibraryComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ControlViewLibraryComponent]
    });
    fixture = TestBed.createComponent(ControlViewLibraryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
