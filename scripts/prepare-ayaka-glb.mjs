#!/usr/bin/env node
// Convert VRM 1.0's GLB container to core glTF 2.0 for Filament gltfio.
// Core geometry, skin joints, morph targets, textures and PBR fields are retained.
// VRM-only extension payloads are removed; expression semantics are not yet mapped.
import fs from 'node:fs';
import path from 'node:path';
const [input, output] = process.argv.slice(2);
if (!input || !output) throw new Error('Usage: prepare-ayaka-glb.mjs input.vrm output.glb');
const bytes = fs.readFileSync(input);
if (bytes.toString('ascii', 0, 4) !== 'glTF' || bytes.readUInt32LE(4) !== 2) throw new Error('Expected GLB version 2.');
if (bytes.readUInt32LE(8) !== bytes.length) throw new Error('GLB declared length mismatch.');
let offset = 12, jsonChunk = null;
const otherChunks = [];
while (offset < bytes.length) {
  const length = bytes.readUInt32LE(offset), type = bytes.readUInt32LE(offset + 4);
  const chunk = bytes.subarray(offset + 8, offset + 8 + length);
  if (chunk.length !== length) throw new Error('Truncated GLB chunk.');
  if (type === 0x4E4F534A) jsonChunk = chunk; else otherChunks.push({ type, data: chunk });
  offset += 8 + length;
}
if (!jsonChunk) throw new Error('GLB JSON chunk missing.');
const doc = JSON.parse(jsonChunk.toString('utf8').trim());
const removed = new Set();
function clean(value) {
  if (Array.isArray(value)) return value.map(clean);
  if (!value || typeof value !== 'object') return value;
  for (const key of Object.keys(value)) {
    if (key === 'extensions') {
      for (const name of Object.keys(value.extensions || {})) removed.add(name);
      delete value.extensions;
    } else value[key] = clean(value[key]);
  }
  return value;
}
clean(doc);
doc.extensionsUsed = (doc.extensionsUsed || []).filter(name => !removed.has(name));
doc.extensionsRequired = (doc.extensionsRequired || []).filter(name => !removed.has(name));
if (!doc.extensionsUsed.length) delete doc.extensionsUsed;
if (!doc.extensionsRequired.length) delete doc.extensionsRequired;
const json = Buffer.from(JSON.stringify(doc), 'utf8');
const padded = Buffer.alloc(Math.ceil(json.length / 4) * 4, 0x20); json.copy(padded);
const chunks = [{ type: 0x4E4F534A, data: padded }, ...otherChunks];
const total = 12 + chunks.reduce((sum, c) => sum + 8 + c.data.length, 0);
const header = Buffer.alloc(12); header.write('glTF', 0, 'ascii'); header.writeUInt32LE(2, 4); header.writeUInt32LE(total, 8);
const parts = [header];
for (const c of chunks) { const h = Buffer.alloc(8); h.writeUInt32LE(c.data.length, 0); h.writeUInt32LE(c.type, 4); parts.push(h, c.data); }
fs.mkdirSync(path.dirname(output), { recursive: true });
fs.writeFileSync(output, Buffer.concat(parts));
console.log('Prepared GLB; stripped extension payloads:', [...removed].join(', ') || '(none)');
