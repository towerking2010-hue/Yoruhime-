#!/usr/bin/env node
// Verify that conversion preserved Ayaka's actual skinned, expressive avatar.
import assert from 'node:assert/strict';
import fs from 'node:fs';

const glbPath = process.argv[2] || 'reborn/src/main/assets/ayaka.glb';
const metaPath = process.argv[3] || 'reborn/src/main/assets/ayaka-vrm-metadata.json';
const bytes = fs.readFileSync(glbPath);
assert.equal(bytes.toString('ascii', 0, 4), 'glTF', 'Ayaka GLB magic is invalid.');
assert.equal(bytes.readUInt32LE(4), 2, 'Ayaka GLB must be glTF 2.0.');
assert.equal(bytes.readUInt32LE(8), bytes.length, 'Ayaka GLB declared size mismatch.');

let offset = 12;
let jsonChunk;
while (offset < bytes.length) {
  const length = bytes.readUInt32LE(offset);
  const type = bytes.readUInt32LE(offset + 4);
  const chunk = bytes.subarray(offset + 8, offset + 8 + length);
  assert.equal(chunk.length, length, 'Truncated GLB chunk.');
  if (type === 0x4E4F534A) jsonChunk = chunk;
  offset += 8 + length;
}
assert.ok(jsonChunk, 'Ayaka GLB JSON chunk is missing.');
const gltf = JSON.parse(jsonChunk.toString('utf8').trim());
assert.ok(gltf.meshes?.length >= 3, 'Expected Ayaka face, body, and hair meshes.');
assert.ok(gltf.skins?.length >= 1, 'Ayaka humanoid skinning data was lost.');
assert.ok(gltf.nodes?.some(node => Number.isInteger(node.skin)), 'No skinned mesh nodes found.');
assert.ok(gltf.animations?.some(animation => animation.name === 'Ayaka_Idle'),
  'The generated Ayaka_Idle animation clip is missing.');
const idle = gltf.animations.find(animation => animation.name === 'Ayaka_Idle');
assert.ok(idle.channels.length >= 2, 'Ayaka idle clip must animate both upper arms.');
assert.ok(idle.channels.every(channel => channel.target?.path === 'rotation'),
  'Ayaka idle clip contains an unexpected animation target.');
const faceClips = gltf.animations.filter(animation => animation.name.startsWith('Ayaka_Face_'));
assert.ok(faceClips.length >= 1, 'Expected at least a generated facial morph animation clip.');
assert.ok(faceClips.every(animation => animation.channels.some(channel => channel.target?.path === 'weights')),
  'Facial clips must animate morph target weights.');
assert.ok(gltf.images?.length >= 1, 'Ayaka textures were lost.');
assert.ok(gltf.materials?.length >= 1, 'Ayaka materials were lost.');
assert.ok(gltf.extensionsUsed?.includes('KHR_materials_unlit'), 'Unlit material extension was lost.');
assert.ok(gltf.extensionsUsed?.includes('KHR_texture_transform'), 'Texture transform extension was lost.');
const morphTargets = gltf.meshes.flatMap(mesh => mesh.primitives || [])
  .reduce((max, primitive) => Math.max(max, primitive.targets?.length || 0), 0);
assert.ok(morphTargets >= 50, 'Ayaka facial morph targets were lost or truncated.');

const metadata = JSON.parse(fs.readFileSync(metaPath, 'utf8'));
const vrm = metadata.extensions?.find(item => item.name === 'VRMC_vrm' && item.path === 'root')?.payload;
assert.ok(vrm?.meta?.authors?.includes('epic_noobster'), 'Ayaka creator attribution was not preserved.');
assert.ok(vrm?.humanoid?.humanBones, 'Ayaka humanoid bone map was not preserved.');
assert.ok(vrm?.expressions, 'Ayaka VRM expression definitions were not preserved.');
assert.ok(metadata.extensions.some(item => item.name === 'VRMC_springBone'), 'Ayaka spring-bone metadata was not preserved.');

console.log(`Ayaka GLB verified: ${gltf.meshes.length} meshes, ${gltf.skins.length} skins, ${morphTargets} morph targets per expressive primitive, ${gltf.images.length} images, ${idle.channels.length} idle channels, ${faceClips.length} facial clips.`);
