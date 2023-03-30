import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DiagramLibraryComponent } from './diagram-library.component';

describe('DiagramLibraryComponent', () => {
  let component: DiagramLibraryComponent;
  let fixture: ComponentFixture<DiagramLibraryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ DiagramLibraryComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(DiagramLibraryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
