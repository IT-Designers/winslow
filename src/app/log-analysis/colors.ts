function oldHslToRgb(hue, sat, light) {
  hue = hue % 360;

  if (hue < 0) {
    hue += 360;
  }

  sat /= 100;
  light /= 100;

  function f(n) {
    let k = (n + hue / 30) % 12;
    let a = sat * Math.min(light, 1 - light);
    return light - a * Math.max(-1, Math.min(k - 3, 9 - k, 1));
  }

  return [f(0), f(8), f(4)];
}

function hslToRgb(hue, sat, light) {
  hue = hue % 360;

  if (hue < 0) {
    hue += 360;
  }

  sat /= 100;
  light /= 100;

  function f(n) {
    let k = (n + hue / 30) % 12;
    let a = sat * Math.min(light, 1 - light);
    const normalised = light - a * Math.max(-1, Math.min(k - 3, 9 - k, 1));
    return Math.round(normalised * 256)
  }

  return [f(0), f(8), f(4)];
}

function rgbToHexString(red: number, green: number, blue: number) {
  const radix = 16
  const redHex = Number(red).toString(radix)
  const greenHex = Number(green).toString(radix)
  const blueHex = Number(blue).toString(radix)
  return `#${redHex}${greenHex}${blueHex}`
}

export function getColor(step: number) {
  const limit = 360
  const offset = 210
  const increment = 277
  const lightness = 60
  const saturation = 80

  const hue = (offset + step * increment) % limit
  const [red, green, blue] = hslToRgb(hue, saturation, lightness)
  return rgbToHexString(red, green, blue)
}
