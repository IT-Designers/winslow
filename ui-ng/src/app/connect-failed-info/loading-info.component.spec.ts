import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { LoadingInfoComponent } from './loading-info.component';

describe('LoadingInfoComponent', () => {
  let component: LoadingInfoComponent;
  let fixture: ComponentFixture<LoadingInfoComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ LoadingInfoComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(LoadingInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
