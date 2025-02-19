import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArgumentListComponent } from './argument-list.component';

describe('ArgumentListComponent', () => {
  let component: ArgumentListComponent;
  let fixture: ComponentFixture<ArgumentListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArgumentListComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ArgumentListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
