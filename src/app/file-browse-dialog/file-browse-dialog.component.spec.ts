import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { FileBrowseDialog } from './file-browse-dialog.component';

describe('FileBrowseComponent', () => {
  let component: FileBrowseDialog;
  let fixture: ComponentFixture<FileBrowseDialog>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ FileBrowseDialog ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FileBrowseDialog);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
