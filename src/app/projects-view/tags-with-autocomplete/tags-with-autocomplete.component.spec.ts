import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { TagsWithAutocompleteComponent } from './tags-with-autocomplete.component';

describe('TagsWithAutocompleteComponent', () => {
  let component: TagsWithAutocompleteComponent;
  let fixture: ComponentFixture<TagsWithAutocompleteComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ TagsWithAutocompleteComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TagsWithAutocompleteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
