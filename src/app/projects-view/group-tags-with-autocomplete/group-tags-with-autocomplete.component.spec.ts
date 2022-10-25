import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GroupTagsWithAutocompleteComponent } from './group-tags-with-autocomplete.component';

describe('GroupTagsWithAutocompleteComponent', () => {
  let component: GroupTagsWithAutocompleteComponent;
  let fixture: ComponentFixture<GroupTagsWithAutocompleteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ GroupTagsWithAutocompleteComponent ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(GroupTagsWithAutocompleteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
