import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ResourceLimitationComponent } from './resource-limitation.component';

describe('ResourceLimitationComponent', () => {
  let component: ResourceLimitationComponent;
  let fixture: ComponentFixture<ResourceLimitationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ResourceLimitationComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ResourceLimitationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
