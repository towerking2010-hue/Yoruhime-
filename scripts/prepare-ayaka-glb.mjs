#!/usr/bin/env node
// Convert VRM 1.0's GLB container to core glTF 2.0 for Filament gltfio.
// Core geometry, skin joints, morph targets, textures and supported KHR extensions are retained.
// VRMC extension payloads are preserved in a sidecar, then removed from the GLB.
import fs from 'node:fs';
import path from 'node:path';

const [input, output] = process.argv.slice(2);
if (!input || !output) throw new Error('Usage: prepare-ayaka-glb.mjs input.vrm output.glb');

const bytes = fs.readFileSync(input);
if (bytes.toString('ascii', 0, 4) !== 'glTF' || bytes.readUInt32LE(4) !== 2) {
  throw new Error('Expected GLB version 2.');
}
if (bytes.readUInt32LE(8) !== bytes.length) throw new Error('GLB declared length mismatch.');

let offset = 12;
let jsonChunk = null;
const otherChunks = [];
while (offset < bytes.length) {
  if (offset + 8 > bytes.length) throw new Error('Truncated GLB chunk header.');
  const length = bytes.readUInt32LE(offset);
  const type = bytes.readUInt32LE(offset + 4);
  const chunk = bytes.subarray(offset + 8, offset + 8 + length);
  if (chunk.length !== length) throw new Error('Truncated GLB chunk.');
  if (type === 0x4E4F534A) {
    if (jsonChunk) throw new Error('GLB contains more than one JSON chunk.');
    jsonChunk = chunk;
  } else {
    otherChunks.push({ type, data: chunk });
  }
  offset += 8 + length;
}
if (!jsonChunk) throw new Error('GLB JSON chunk missing.');

const doc = JSON.parse(jsonChunk.toString('utf8').trim());

// Capture every VRMC extension payload with its JSON path. This preserves
// creator/license, humanoid, expressions, MToon and spring-bone data for future use.
const vrmExtensions = [];
function collectVrmExtensions(value, currentPath = 'root') {
  if (Array.isArray(value)) {
    value.forEach((item, index) => collectVrmExtensions(item, currentPath + '[' + index + ']'));
    return;
  }
  if (!value || typeof value !== 'object') return;

  if (value.extensions && typeof value.extensions === 'object') {
    for (const [name, payload] of Object.entries(value.extensions)) {
      if (name.startsWith('VRMC_')) {
        vrmExtensions.push({
          path: currentPath,
          name,
          payload: JSON.parse(JSON.stringify(payload))
        });
      }
    }
  }

  for (const [key, child] of Object.entries(value)) {
    if (key !== 'extensions' && child && typeof child === 'object') {
      collectVrmExtensions(child, currentPath + '.' + key);
    }
  }
}
collectVrmExtensions(doc);

const vrmMetadata = vrmExtensions.find(item => item.name === 'VRMC_vrm' && item.path === 'root')?.payload?.meta;
if (!vrmMetadata || typeof vrmMetadata !== 'object') {
  throw new Error('VRM metadata missing; refusing to discard creator and license attribution.');
}
const preservedVrm = {
  sourceModel: path.basename(input),
  extensions: vrmExtensions
};

const removed = new Set();
const isVrmExtension = name => name.startsWith('VRMC_');
function clean(value) {
  if (Array.isArray(value)) return value.map(clean);
  if (!value || typeof value !== 'object') return value;

  if (value.extensions && typeof value.extensions === 'object') {
    for (const name of Object.keys(value.extensions)) {
      if (isVrmExtension(name)) {
        removed.add(name);
        delete value.extensions[name];
      }
    }
    if (Object.keys(value.extensions).length === 0) delete value.extensions;
  }

  for (const key of Object.keys(value)) {
    if (key !== 'extensions') value[key] = clean(value[key]);
  }
  return value;
}
clean(doc);

// Filament gltfio supports standard KHR extensions such as unlit materials and
// texture transforms. Remove only VRM-specific declarations, not valid glTF data.
doc.extensionsUsed = (doc.extensionsUsed || []).filter(name => !isVrmExtension(name));
doc.extensionsRequired = (doc.extensionsRequired || []).filter(name => !isVrmExtension(name));
if (!doc.extensionsUsed.length) delete doc.extensionsUsed;
if (!doc.extensionsRequired.length) delete doc.extensionsRequired;

const json = Buffer.from(JSON.stringify(doc), 'utf8');
const padded = Buffer.alloc(Math.ceil(json.length / 4) * 4, 0x20);
json.copy(padded);

const chunks = [{ type: 0x4E4F534A, data: padded }, ...otherChunks];
const total = 12 + chunks.reduce((sum, chunk) => sum + 8 + chunk.data.length, 0);
const header = Buffer.alloc(12);
header.write('glTF', 0, 'ascii');
header.writeUInt32LE(2, 4);
header.writeUInt32LE(total, 8);

const parts = [header];
for (const chunk of chunks) {
  const chunkHeader = Buffer.alloc(8);
  chunkHeader.writeUInt32LE(chunk.data.length, 0);
  chunkHeader.writeUInt32LE(chunk.type, 4);
  parts.push(chunkHeader, chunk.data);
}

fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, Buffer.concat(parts));
const metadataOutput = path.join(path.dirname(output), 'ayaka-vrm-metadata.json');
fs.writeFileSync(metadataOutput, JSON.stringify(preservedVrm, null, 2) + '\n');

console.log('Preserved VRM metadata extensions:', [...new Set(vrmExtensions.map(item => item.name))].join(', '));
console.log('Preserved VRM creator:', vrmMetadata.authors?.join(', ') || '(no author listed)');
console.log('Prepared GLB; stripped extension payloads:', [...removed].join(', ') || '(none)');
