import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServersNewComponent } from './servers-new.component';

describe('ServersNewComponent', () => {
  let component: ServersNewComponent;
  let fixture: ComponentFixture<ServersNewComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ServersNewComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServersNewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
