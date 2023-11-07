import {Component, Inject, OnInit} from '@angular/core';
import {ProjectApiService, ProjectGroup} from '../../api/project-api.service';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import {DialogService} from '../../dialog.service';
import {ProjectInfo} from "../../api/winslow-api";

@Component({
  selector: 'app-add-to-context-popup',
  templateUrl: './add-to-context-popup.component.html',
  styleUrls: ['./add-to-context-popup.component.css']
})
export class AddToContextPopupComponent implements OnInit {

  proposals: string[] = [];
  cachedTags: string[];
  previewTags: string[] = [];
  CONTEXT_PREFIX = 'context::';

  constructor(
    public api: ProjectApiService,
    private dialog: DialogService,
    @Inject(MAT_DIALOG_DATA) public data: AddToContextDialogData
  ) {
    this.cachedTags = this.api.cachedTags;
    if (this.data.project) {
      this.previewTags = this.data.project.tags
        .filter(tag => tag.startsWith(this.CONTEXT_PREFIX));
    }
    this.cachedTags.forEach(tag => {
      if (tag.startsWith(this.CONTEXT_PREFIX)) {
        this.proposals.push(tag);
      }
    });
  }

  ngOnInit(): void {

  }

  setTagsForProject(project: ProjectInfo, tags: string[]) {
    project.tags.forEach(tag => {
      if (!tags.includes(tag)) {
        tags.push(tag);
      }
    });
    return this.dialog.openLoadingIndicator(
      this.api
        .setTags(project.id, tags)
        .then(result => {
          project.tags = tags;
        }),
      'Updating tags'
    );
  }

  setTagsForGroup(projectGroup: ProjectGroup, tags: string[]) {
    projectGroup.projects.forEach(project => {
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

type AddToContextDialogData = {
  project: ProjectInfo
  projectGroup: undefined
} | {
  project: undefined
  projectGroup: ProjectGroup
}
