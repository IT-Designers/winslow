# WinslowUiNg

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 8.2.1.

Node version >=18.13 required

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build
Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

### Workaround for outdated dependencies
We use [ngx-sweetalert2](https://github.com/sweetalert2/ngx-sweetalert2) which is currently not supported for Angular 17.
<br>
What was done to solve this problem:
1. There is an open [pull-request](https://github.com/kjra1707/ngx-sweetalert2/tree/patch-1) which makes the project work with Angular 17.
1. We use [git-subtree](https://www.atlassian.com/git/tutorials/git-subtree) to clone the project into the ui-ng folder.
   * `git subtree add --prefix ui-ng/.subtree/ngx-sweetalert2 https://github.com/kjra1707/ngx-sweetalert2.git patch-1 --squash`
1. adjust the [package.json](package.json) with a preinstall step, which will be run before `npm install` is executed
   * `"preinstall": "cd .subtree/ngx-sweetalert2; npm install && npm run build"`
1. at least we use the local builded `ngx-sweetalert2` as local dependency in [package.json](package.json)
   * `@sweetalert2/ngx-sweetalert2": ".subtree/ngx-sweetalert2/dist/ngx-sweetalert2`
> Use this approach if another outdated dependency is detected


## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
