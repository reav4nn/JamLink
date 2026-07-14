const sharp = require('sharp');
const fs = require('fs');
const path = require('path');

const svgSource = path.join(__dirname, 'jamlink-logo-source.svg');
const androidResPath = path.join(__dirname, '../../android/app/src/main/res');
const assetsPath = path.join(__dirname, '../../assets');
const iosIconSetPath = path.join(__dirname, '../../ios/JamLink/Images.xcassets/AppIcon.appiconset');

const androidSizes = {
  mdpi: 108,
  hdpi: 162,
  xhdpi: 216,
  xxhdpi: 324,
  xxxhdpi: 432
};

const iosSizes = [
  { size: 20, scales: [1, 2, 3] },
  { size: 29, scales: [1, 2, 3] },
  { size: 40, scales: [1, 2, 3] },
  { size: 60, scales: [2, 3] },
  { size: 76, scales: [1, 2] },
  { size: 83.5, scales: [2] },
  { size: 1024, scales: [1] }
];

async function generate() {
  try {
    // 1. Create the Workflow-specific assets/app-icon.icon structure for iOS 26 Liquid Glass
    console.log('Creating assets/app-icon.icon for iOS 26 workflow...');
    const iconAssetsDir = path.join(assetsPath, 'app-icon.icon/Assets');
    if (!fs.existsSync(iconAssetsDir)) {
      fs.mkdirSync(iconAssetsDir, { recursive: true });
    }
    
    // The main icon is 1024x1024 with transparent background
    await sharp(svgSource)
      .resize(1024, 1024)
      .toFile(path.join(iconAssetsDir, 'icon.png'));

    const iconJson = {
      "fill": "automatic",
      "groups": [{
        "layers": [{"glass": false, "image-name": "icon.png", "name": "icon"}],
        "shadow": {"kind": "neutral", "opacity": 0.5},
        "translucency": {"enabled": true, "value": 0.5}
      }],
      "supported-platforms": {"circles": ["watchOS"], "squares": "shared"}
    };
    fs.writeFileSync(path.join(assetsPath, 'app-icon.icon/icon.json'), JSON.stringify(iconJson, null, 2));

    // Create a 1024x1024 base icon for legacy processing
    const baseIconBuffer = await sharp(svgSource).resize(1024, 1024).toBuffer();
    
    // Create the workflow's 66% Android-optimized asset
    console.log('Creating android-icon.png (66% scaled)...');
    await sharp(baseIconBuffer)
      .resize(Math.round(1024 * 0.66), Math.round(1024 * 0.66))
      .extend({
        top: Math.round(1024 * 0.17),
        bottom: Math.round(1024 * 0.17),
        left: Math.round(1024 * 0.17),
        right: Math.round(1024 * 0.17),
        background: { r: 0, g: 0, b: 0, alpha: 0 }
      })
      .resize(1024, 1024)
      .toFile(path.join(assetsPath, 'android-icon.png'));

    // 2. Generate native Android Adaptive Icons (Foreground 66% + Background)
    console.log('Generating native Android icons...');
    for (const [density, size] of Object.entries(androidSizes)) {
      const outDir = path.join(androidResPath, `mipmap-${density}`);
      if (!fs.existsSync(outDir)) {
        fs.mkdirSync(outDir, { recursive: true });
      }

      const innerSize = Math.round(size * 0.66);
      const padding = Math.round((size - innerSize) / 2);

      // Adaptive foreground
      await sharp(baseIconBuffer)
        .resize(innerSize, innerSize)
        .extend({
          top: padding, bottom: padding, left: padding, right: padding,
          background: { r: 0, g: 0, b: 0, alpha: 0 }
        })
        .resize(size, size)
        .toFile(path.join(outDir, 'ic_launcher_foreground.png'));

      // Legacy full square icon (adding background explicitly for legacy)
      await sharp(baseIconBuffer)
        .resize(size, size)
        .flatten({ background: '#141413' })
        .toFile(path.join(outDir, 'ic_launcher.png'));

      // Legacy round icon
      const circleSvg = Buffer.from(`<svg><circle cx="${size/2}" cy="${size/2}" r="${size/2}" /></svg>`);
      await sharp(baseIconBuffer)
        .resize(size, size)
        .flatten({ background: '#141413' })
        .composite([{ input: circleSvg, blend: 'dest-in' }])
        .toFile(path.join(outDir, 'ic_launcher_round.png'));
    }

    const bgXmlPath = path.join(androidResPath, 'values/ic_launcher_background.xml');
    if (fs.existsSync(bgXmlPath)) {
       let bgXml = fs.readFileSync(bgXmlPath, 'utf8');
       bgXml = bgXml.replace(/#808080|#0D1117/, '#141413');
       fs.writeFileSync(bgXmlPath, bgXml);
    }

    // 3. Generate native iOS Icons
    console.log('Generating native iOS icons...');
    if (fs.existsSync(iosIconSetPath)) {
      let contentsJson = { images: [], info: { author: "xcode", version: 1 } };
      
      for (const item of iosSizes) {
        for (const scale of item.scales) {
          const pixelSize = Math.round(item.size * scale);
          const filename = `icon-${item.size}x${item.size}@${scale}x.png`;
          
          await sharp(baseIconBuffer)
            .resize(pixelSize, pixelSize)
            .flatten({ background: '#141413' })
            .toFile(path.join(iosIconSetPath, filename));
            
          contentsJson.images.push({
            size: `${item.size}x${item.size}`,
            idiom: item.size === 1024 ? "ios-marketing" : (item.size >= 76 ? "ipad" : "iphone"),
            filename: filename,
            scale: `${scale}x`
          });
        }
      }
      
      fs.writeFileSync(path.join(iosIconSetPath, 'Contents.json'), JSON.stringify(contentsJson, null, 2));
    } else {
      console.log('Skipping iOS icons (ios/JamLink/Images.xcassets not found).');
    }

    // 4. Generate Play Store 512x512 Icon
    console.log('Generating Play Store 512x512 icon...');
    await sharp(baseIconBuffer)
      .resize(512, 512)
      .flatten({ background: '#141413' })
      .toFile(path.join(__dirname, 'play-store-icon-512.png'));

    console.log('All icons generated and configured successfully!');
  } catch (error) {
    console.error('Error generating icons:', error);
  }
}

generate();
