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
  return `#${redHex}${blueHex}${greenHex}`
}

export function getColorSet(amount: number) {
  const limit = 360
  const offset = 210
  const increment = limit / amount
  const lightness = 50
  const saturation = 50

  let results = []

  for (let i = 0; i < amount; i++) {
    const hue = (offset + i * increment) % limit
    console.log(hue)
    const [red, green, blue] = hslToRgb(hue, saturation, lightness)
    const hex = rgbToHexString(red, green, blue)
    console.log(hex)
    results.push(hex)
  }

  console.log(results)
  return results
}
