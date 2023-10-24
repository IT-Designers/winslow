// Fixes the "Could not find a declaration file for module 'echarts'" in app.module.ts.
// I do not know why the declaration file in /node_modules/@types/echarts/index.d.ts does not solve this.

declare module 'echarts' {
  function init(
    dom: HTMLDivElement | HTMLCanvasElement,
    theme?: object | string,
    opts?: {
      devicePixelRatio?: number | undefined;
      renderer?: string | undefined;
      width?: number | string | undefined;
      height?: number | string | undefined;
    },
  ): ECharts;
}
