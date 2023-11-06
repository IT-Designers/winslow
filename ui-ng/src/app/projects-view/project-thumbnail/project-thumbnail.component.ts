import {Component, Input, OnInit} from '@angular/core';
import {ProjectInfo} from "../../api/winslow-api";
import {FilesApiService} from "../../api/files-api.service";
import {DialogService} from "../../dialog.service";
import {lastValueFrom} from "rxjs";

@Component({
  selector: 'app-project-avatar',
  templateUrl: './project-thumbnail.component.html',
  styleUrls: ['./project-thumbnail.component.css']
})
export class ProjectThumbnailComponent implements OnInit {
  @Input() project!: ProjectInfo;

  image?: HTMLImageElement;

  alternativeText: string = '?';

  constructor(
    private filesApi: FilesApiService,
    private dialog: DialogService
  ) {
  }

  ngOnInit() {
    const filepath = `${this.project.id}/output/thumbnail.jpg`; // Can be any image type supported by HTMLImageElements, not just JPGs
    const url = this.filesApi.workspaceUrl(filepath);
    this.alternativeText = this.getAlternativeText(this.project.name);
    this.getThumbnailImage(url).then(image => this.image = image);
  }

  showFullThumbnail(imageUrl: string) {
    this.dialog.image(imageUrl);
  }

  private getThumbnailImage(url: string): Promise<HTMLImageElement | undefined> {
    return new Promise<HTMLImageElement | undefined>(resolve => {
      const image = new Image();
      image.onload = () => {
        console.log(image)
        resolve(image);
      };
      image.onerror = () => {
        console.log("FAILURE")
        resolve(undefined);
      };
      image.src = url;
      console.log(image)
    })
  }

  private getAlternativeText(name: string) {
    const words = name.trim().split(" ");
    switch (words.length) {
      case 0:
        return "?"
      case 1:
        return words[0].substring(0, 1).toUpperCase();
      default:
        return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase();
    }
  }
}
