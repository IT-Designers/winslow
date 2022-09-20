import {
  OnInit,
  Component,
  Input
} from '@angular/core';

import {HttpClient} from '@angular/common/http';

import BpmnModeler from 'bpmn-js/lib/Modeler.js';
import * as BpmnJS from 'bpmn-js/dist/bpmn-modeler.production.min.js';
import {BpmnPropertiesPanelModule, BpmnPropertiesProviderModule} from 'bpmn-js-properties-panel';

import {Observable} from 'rxjs';
import {ProjectInfo} from "../../api/project-api.service";

@Component({
  selector: 'app-diagram',
  templateUrl: './diagram.component.html',
  styleUrls: ['./diagram.component.css']
})
export class DiagramComponent implements OnInit {
  modeler: BpmnModeler;
  @Input() projectDef: ProjectInfo;

  constructor(private http: HttpClient) {
  }

  ngOnInit(): void {
    this.modeler = new BpmnModeler({
      container: '#js-canvas',
      width: '100%',
      height: '100%',
      propertiesPanel: {
        parent: '#js-properties-panel'
      },
      additionalModules: [
        BpmnPropertiesPanelModule,
        BpmnPropertiesProviderModule
      ],
    });
    this.load();
  }

  load(): void {
    console.dir(this.projectDef.pipelineDefinition);
    this.getExample().subscribe(data => {
      this.modeler.importXML(data, value => this.handleError(value));
      console.log(this.modeler.get('moddle'));
    });
  }

  handleError(err: any) {
    if (err) {
      console.warn('XML Import Error:' + err);
    }
  }

  public getExample(): Observable<string> {
    const url = 'https://cdn.staticaly.com/gh/bpmn-io/bpmn-js-examples/dfceecba/starter/diagram.bpmn';
    return this.http.get(url, {responseType: 'text'});
  }
}
