#!/usr/bin/env node
// Convert VRM 1.0 GLB to core glTF 2.0. Preserve VRMC data in a sidecar.
// Bake a conservative procedural idle clip into the GLB for Filament's gltfio Animator.
import fs from 'node:fs';
import path from 'node:path';

const [input, output] = process.argv.slice(2);
if (!input || !output) throw new Error('Usage: prepare-ayaka-glb.mjs input.vrm output.glb');
const bytes = fs.readFileSync(input);
if (bytes.toString('ascii', 0, 4) !== 'glTF' || bytes.readUInt32LE(4) !== 2) throw new Error('Expected GLB version 2.');
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
  } else otherChunks.push({ type, data: chunk });
  offset += 8 + length;
}
if (!jsonChunk) throw new Error('GLB JSON chunk missing.');
const doc = JSON.parse(jsonChunk.toString('utf8').trim());

const vrmExtensions = [];
function collectVrmExtensions(value, currentPath = 'root') {
  if (Array.isArray(value)) {
    value.forEach((item, index) => collectVrmExtensions(item, currentPath + '[' + index + ']'));
    return;
  }
  if (!value || typeof value !== 'object') return;
  if (value.extensions && typeof value.extensions === 'object') {
    for (const [name, payload] of Object.entries(value.extensions)) {
      if (name.startsWith('VRMC_')) vrmExtensions.push({
        path: currentPath, name, payload: JSON.parse(JSON.stringify(payload))
      });
    }
  }
  for (const [key, child] of Object.entries(value)) {
    if (key !== 'extensions' && child && typeof child === 'object') {
      collectVrmExtensions(child, currentPath + '.' + key);
    }
  }
}
collectVrmExtensions(doc);
const vrmPayload = vrmExtensions.find(item => item.name === 'VRMC_vrm' && item.path === 'root')?.payload;
const vrmMetadata = vrmPayload?.meta;
if (!vrmMetadata || typeof vrmMetadata !== 'object') throw new Error('VRM metadata missing; refusing to discard creator and license attribution.');
const preservedVrm = { sourceModel: path.basename(input), extensions: vrmExtensions };

function addProceduralIdle(gltf, binaryChunks, humanoid) {
  const bones = humanoid?.humanBones || {};
  const boneNode = name => Number.isInteger(bones[name]?.node) ? bones[name].node : null;
  const available = {
    leftUpperArm: boneNode('leftUpperArm'),
    rightUpperArm: boneNode('rightUpperArm'),
    spine: boneNode('spine'),
    chest: boneNode('chest') ?? boneNode('upperChest'),
    neck: boneNode('neck'),
    head: boneNode('head')
  };
  const nodes = gltf.nodes || [];
  const targets = Object.entries(available).filter(([name, node]) =>
    node !== null && node >= 0 && node < nodes.length && nodes[node]
  );
  if (!targets.some(([name]) => name === 'leftUpperArm') || !targets.some(([name]) => name === 'rightUpperArm')) {
    throw new Error('VRM humanoid map is missing leftUpperArm/rightUpperArm; cannot safely generate idle pose.');
  }

  const multiply = (a, b) => [
    a[3]*b[0] + a[0]*b[3] + a[1]*b[2] - a[2]*b[1],
    a[3]*b[1] - a[0]*b[2] + a[1]*b[3] + a[2]*b[0],
    a[3]*b[2] + a[0]*b[1] - a[1]*b[0] + a[2]*b[3],
    a[3]*b[3] - a[0]*b[0] - a[1]*b[1] - a[2]*b[2]
  ];
  const axisAngle = (axis, angle) => {
    const half = angle / 2, s = Math.sin(half);
    return [axis[0]*s, axis[1]*s, axis[2]*s, Math.cos(half)];
  };
  const normalize = q => { const n = Math.hypot(...q); return q.map(v => v / (n || 1)); };
  const times = [0, 1, 2, 3, 4];
  const channels = [], samplers = [];
  const binary = binaryChunks.find(chunk => chunk.type === 0x004E4942);
  let binData = binary?.data || Buffer.alloc(0);
  if (!gltf.buffers) gltf.buffers = [{ byteLength: binData.length }];
  if (!gltf.buffers[0]) gltf.buffers[0] = { byteLength: binData.length };
  if (gltf.buffers.length !== 1 || gltf.buffers[0].uri) throw new Error('Expected one embedded GLB buffer.');

  const appendAccessor = (values, type, min, max) => {
    const pad = (4 - (binData.length % 4)) % 4;
    if (pad) binData = Buffer.concat([binData, Buffer.alloc(pad)]);
    const bufferOffset = binData.length;
    const payload = Buffer.alloc(values.length * 4);
    values.forEach((value, index) => payload.writeFloatLE(value, index * 4));
    binData = Buffer.concat([binData, payload]);
    const bufferView = (gltf.bufferViews ||= []).push({
      buffer: 0, byteOffset: bufferOffset, byteLength: payload.length
    }) - 1;
    return (gltf.accessors ||= []).push({
      bufferView, byteOffset: 0, componentType: 5126,
      count: type === 'SCALAR' ? values.length : values.length / 4,
      type, ...(min ? { min } : {}), ...(max ? { max } : {})
    }) - 1;
  };
  const timeAccessor = appendAccessor(times, 'SCALAR', [0], [4]);
  for (const [name, nodeIndex] of targets) {
    const base = nodes[nodeIndex].rotation || [0, 0, 0, 1];
    const rotations = times.map(time => {
      let delta = [0, 0, 0, 1];
      const phase = time / 4 * Math.PI * 2;
      if (name === 'leftUpperArm') delta = axisAngle([0, 0, 1], -1.25);
      else if (name === 'rightUpperArm') delta = axisAngle([0, 0, 1], 1.25);
      else if (name === 'spine') delta = axisAngle([1, 0, 0], Math.sin(phase) * 0.012);
      else if (name === 'chest') delta = axisAngle([1, 0, 0], Math.sin(phase) * 0.022);
      else if (name === 'neck') delta = axisAngle([0, 1, 0], Math.sin(phase + 0.7) * 0.018);
      else if (name === 'head') {
        delta = multiply(axisAngle([0, 1, 0], Math.sin(phase + 1.1) * 0.028),
          axisAngle([1, 0, 0], Math.sin(phase + 0.2) * 0.012));
      }
      return normalize(multiply(base, delta));
    });
    const outputAccessor = appendAccessor(rotations.flat(), 'VEC4');
    const sampler = samplers.push({ input: timeAccessor, output: outputAccessor, interpolation: 'LINEAR' }) - 1;
    channels.push({ sampler, target: { node: nodeIndex, path: 'rotation' } });
  }
  gltf.animations = (gltf.animations || []).filter(animation => animation.name !== 'Ayaka_Idle');
  gltf.animations.push({ name: 'Ayaka_Idle', samplers, channels });
  gltf.buffers[0].byteLength = binData.length;
  if (binary) binary.data = binData;
  else binaryChunks.push({ type: 0x004E4942, data: binData });
  return { channelCount: channels.length, animatedBones: targets.map(([name]) => name) };
}

function addFacialClips(gltf, binaryChunks, expressions) {
  const presets = expressions?.preset || {};
  const blinkBinds = presets.blink?.morphTargetBinds || [];
  const available = Object.entries(presets)
    .filter(([name, value]) => name !== 'blink' && Array.isArray(value?.morphTargetBinds) && value.morphTargetBinds.length)
    .map(([name]) => name);
  const states = ['neutral', ...available];
  const binary = binaryChunks.find(chunk => chunk.type === 0x004E4942);
  let binData = binary?.data || Buffer.alloc(0);
  const append = (values, type) => {
    const pad = (4 - (binData.length % 4)) % 4;
    if (pad) binData = Buffer.concat([binData, Buffer.alloc(pad)]);
    const byteOffset = binData.length;
    const payload = Buffer.alloc(values.length * 4);
    values.forEach((value, index) => payload.writeFloatLE(value, index * 4));
    binData = Buffer.concat([binData, payload]);
    const bufferView = (gltf.bufferViews ||= []).push({ buffer: 0, byteOffset, byteLength: payload.length }) - 1;
    return (gltf.accessors ||= []).push({
      bufferView, byteOffset: 0, componentType: 5126,
      count: type === 'SCALAR' ? values.length : values.length / 4, type
    }) - 1;
  };
  const nodeIndices = new Set([...blinkBinds, ...states.flatMap(name => presets[name]?.morphTargetBinds || [])]
    .map(bind => bind.node).filter(Number.isInteger));
  const nodeTargets = new Map();
  for (const nodeIndex of nodeIndices) {
    const node = gltf.nodes?.[nodeIndex];
    const mesh = Number.isInteger(node?.mesh) ? gltf.meshes?.[node.mesh] : null;
    const count = Math.max(0, ...(mesh?.primitives || []).map(primitive => primitive.targets?.length || 0));
    if (count > 0) nodeTargets.set(nodeIndex, count);
  }
  if (!nodeTargets.size) {
    if (binary) binary.data = binData;
    return [];
  }
  const times = [0, 1.05, 1.13, 1.21, 2.75, 2.83, 2.91, 4];
  const timeAccessor = append(times, 'SCALAR');
  const blinkCurve = [0, 0, 1, 0, 0, 1, 0, 0];
  const generated = [];
  for (const state of states) {
    const baseBinds = presets[state]?.morphTargetBinds || [];
    const channels = [];
    const samplers = [];
    for (const [nodeIndex, targetCount] of nodeTargets) {
      const weights = times.map((_, key) => {
        const frame = new Array(targetCount).fill(0);
        for (const bind of baseBinds) {
          if (bind.node === nodeIndex && Number.isInteger(bind.index) && bind.index >= 0 && bind.index < targetCount) {
            frame[bind.index] = Math.max(0, Math.min(1, Number(bind.weight) || 0));
          }
        }
        for (const bind of blinkBinds) {
          if (bind.node === nodeIndex && Number.isInteger(bind.index) && bind.index >= 0 && bind.index < targetCount) {
            frame[bind.index] = Math.max(frame[bind.index], Math.max(0, Math.min(1, Number(bind.weight) || 0)) * blinkCurve[key]);
          }
        }
        return frame;
      });
      if (!weights.some(frame => frame.some(value => value > 0))) continue;
      const outputAccessor = append(weights.flat(), 'SCALAR');
      const sampler = samplers.push({ input: timeAccessor, output: outputAccessor, interpolation: 'LINEAR' }) - 1;
      channels.push({ sampler, target: { node: nodeIndex, path: 'weights' } });
    }
    if (!channels.length) continue;
    const label = state === 'neutral' ? 'Neutral' : state === 'aa' ? 'Talking' : state[0].toUpperCase() + state.slice(1);
    const name = 'Ayaka_Face_' + label;
    gltf.animations.push({ name, samplers, channels });
    generated.push(name);
  }
  gltf.buffers[0].byteLength = binData.length;
  if (binary) binary.data = binData;
  else binaryChunks.push({ type: 0x004E4942, data: binData });
  return generated;
}
const idle = addProceduralIdle(doc, otherChunks, vrmPayload?.humanoid);
const facialClips = addFacialClips(doc, otherChunks, vrmPayload?.expressions);

const removed = new Set();
const isVrmExtension = name => name.startsWith('VRMC_');
function clean(value) {
  if (Array.isArray(value)) return value.map(clean);
  if (!value || typeof value !== 'object') return value;
  if (value.extensions && typeof value.extensions === 'object') {
    for (const name of Object.keys(value.extensions)) {
      if (isVrmExtension(name)) { removed.add(name); delete value.extensions[name]; }
    }
    if (Object.keys(value.extensions).length === 0) delete value.extensions;
  }
  for (const key of Object.keys(value)) if (key !== 'extensions') value[key] = clean(value[key]);
  return value;
}
clean(doc);
doc.extensionsUsed = (doc.extensionsUsed || []).filter(name => !isVrmExtension(name));
doc.extensionsRequired = (doc.extensionsRequired || []).filter(name => !isVrmExtension(name));
if (!doc.extensionsUsed.length) delete doc.extensionsUsed;
if (!doc.extensionsRequired.length) delete doc.extensionsRequired;

const json = Buffer.from(JSON.stringify(doc), 'utf8');
const paddedJson = Buffer.alloc(Math.ceil(json.length / 4) * 4, 0x20);
json.copy(paddedJson);
const chunks = [{ type: 0x4E4F534A, data: paddedJson }, ...otherChunks];
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
fs.writeFileSync(path.join(path.dirname(output), 'ayaka-vrm-metadata.json'), JSON.stringify(preservedVrm, null, 2) + '\n');
console.log('Preserved VRM metadata extensions:', [...new Set(vrmExtensions.map(item => item.name))].join(', '));
console.log('Preserved VRM creator:', vrmMetadata.authors?.join(', ') || '(no author listed)');
console.log('Generated idle animation:', idle.animatedBones.join(', '), '(' + idle.channelCount + ' channels)');
console.log('Generated facial animation clips:', facialClips.join(', ') || '(no supported VRM morph binds found)');
console.log('Prepared GLB; stripped extension payloads:', [...removed].join(', ') || '(none)');
