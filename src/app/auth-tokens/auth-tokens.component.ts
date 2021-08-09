import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {AuthTokenInfo} from '../api/project-api.service';
import {DialogService} from '../dialog.service';

@Component({
  selector: 'app-auth-tokens',
  templateUrl: './auth-tokens.component.html',
  styleUrls: ['./auth-tokens.component.css']
})
export class AuthTokensComponent implements OnInit {

  @Input() tokens: AuthTokenInfo[] = null;
  @Output('create') createEmitter = new EventEmitter<string>();
  @Output('delete') deleteEmitter = new EventEmitter<AuthTokenInfo>();

  constructor(private dialog: DialogService) { }

  ngOnInit(): void {
  }

  trackToken(token: AuthTokenInfo): string {
    return token.id;
  }

  createToken() {
    this.dialog.createAThing(
      'Auth-Token',
      'Name of the new Auth-Token',
      name => {
        this.createEmitter.emit(name);
        return Promise.resolve();
      }
    );
  }

  deleteToken(token: AuthTokenInfo) {
    this.dialog.openAreYouSure(
      `Deleting the token '${token.name}'`,
      () => {
        this.deleteEmitter.emit(token);
        return Promise.resolve();
      }
    );
  }
}
