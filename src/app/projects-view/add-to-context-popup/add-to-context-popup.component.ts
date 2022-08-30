import {Component, Inject, OnInit} from '@angular/core';
import {ProjectApiService} from '../../api/project-api.service';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {DialogService} from '../../dialog.service';

@Component({
  selector: 'app-add-to-context-popup',
  templateUrl: './add-to-context-popup.component.html',
  styleUrls: ['./add-to-context-popup.component.css']
})
export class AddToContextPopupComponent implements OnInit {

  isGroup: boolean;
  proposals = [];
  cachedTags: string[];
  previewTags: string[];
  CONTEXT_PREFIX = 'context::';

  constructor(public api: ProjectApiService,
              private dialog: DialogService,
              @Inject(MAT_DIALOG_DATA) public data: any) {
  }

  ngOnInit(): void {
    this.cachedTags = this.api.cachedTags;
    if (this.data.projectGroup) {
      this.isGroup = true;
    } else if (this.data.project) {
      this.isGroup = false;
      this.previewTags = this.data.project.tags
        .filter(tag => tag.startsWith(this.CONTEXT_PREFIX));
    }
    this.cachedTags.forEach(tag => {
      if (tag.startsWith(this.CONTEXT_PREFIX)) {
        this.proposals.push(tag);
      }
    });
  }

  setTagsForProject(tags: string[]) {
    this.data.project.tags.forEach(tag => {
      if (!tags.includes(tag)) {
        tags.push(tag);
      }
    });
    return this.dialog.openLoadingIndicator(
      this.api
        .setTags(this.data.project.id, tags)
        .then(result => {
          this.data.project.tags = tags;
        }),
      'Updating tags'
    );
  }

  setTagsForGroup(tags: string[]) {
    this.data.projectGroup.projects.forEach(project => {
      project.tags.forEach(tag => {
        tags.push(tag);
      });
      this.api
        .setTags(project.id, tags)
        .then(result => {
          project.tags = tags;
        });
    });
  }

}
