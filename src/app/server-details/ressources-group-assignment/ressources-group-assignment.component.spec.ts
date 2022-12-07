import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RessourcesGroupAssignmentComponent } from './ressources-group-assignment.component';

describe('RessourcesGroupAssignmentComponent', () => {
  let component: RessourcesGroupAssignmentComponent;
  let fixture: ComponentFixture<RessourcesGroupAssignmentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ RessourcesGroupAssignmentComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(RessourcesGroupAssignmentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
