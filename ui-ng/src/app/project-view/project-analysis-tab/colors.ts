function hslToRgb(hue: number, sat: number, light: number): [number, number, number] {
  hue = hue % 360;

  if (hue < 0) {
    hue += 360;
  }

  sat /= 100;
  light /= 100;

  function hueToRgbChannel(channelOffset: number) {
    let adjustedHue = (channelOffset + hue) % 360;
    let chroma = sat * Math.min(light, 1 - light);
    const normalised = light - chroma * Math.max(-1, Math.min(adjustedHue / 30 - 3, 9 - adjustedHue / 30, 1));
    return Math.round(normalised * 256)
  }

  return [hueToRgbChannel(0), hueToRgbChannel(240), hueToRgbChannel(120)];
}

function rgbToHexString(red: number, green: number, blue: number) {
  const radix = 16
  const redHex = Number(red).toString(radix)
  const greenHex = Number(green).toString(radix)
  const blueHex = Number(blue).toString(radix)
  return `#${redHex}${greenHex}${blueHex}`
}

export function generateColor(step: number) {
  const limit = 360
  const offset = 210
  const increment = 277
  const lightness = 60
  const saturation = 80

  const hue = (offset + step * increment) % limit
  const [red, green, blue] = hslToRgb(hue, saturation, lightness)
  return rgbToHexString(red, green, blue)
}
