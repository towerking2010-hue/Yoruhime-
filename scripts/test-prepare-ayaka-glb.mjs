#!/usr/bin/env node
// Regression test: preserve supported KHR extensions and every VRMC payload.
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptPath = path.join(path.dirname(fileURLToPath(import.meta.url)), 'prepare-ayaka-glb.mjs');
const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'yoruhime-glb-test-'));

try {
  const input = path.join(temp, 'source.vrm');
  const output = path.join(temp, 'ayaka.glb');
  const source = {
    asset: { version: '2.0' },
    nodes: [...Array.from({ length: 7 }, () => ({})), { mesh: 0 }],
    meshes: [{ primitives: [{ targets: [{}, {}] }] }],
    extensionsUsed: [
      'VRMC_vrm',
      'VRMC_springBone',
      'VRMC_materials_mtoon',
      'KHR_materials_unlit',
      'KHR_texture_transform'
    ],
    extensionsRequired: ['VRMC_vrm', 'KHR_materials_unlit'],
    extensions: {
      VRMC_vrm: {
        meta: {
          name: 'Ayaka',
          authors: ['epic_noobster'],
          creditNotation: 'required',
          licenseUrl: 'https://example.invalid/vrm-test-license',
          redistribution: 'test-only'
        },
        humanoid: { humanBones: {
          leftUpperArm: { node: 1 }, rightUpperArm: { node: 2 },
          spine: { node: 3 }, chest: { node: 4 }, neck: { node: 5 }, head: { node: 6 }
        } },
        expressions: {
          preset: {
            happy: { morphTargetBinds: [{ node: 7, index: 0, weight: 1 }] },
            blinkLeft: { morphTargetBinds: [{ node: 7, index: 1, weight: 1 }] },
            blinkRight: { morphTargetBinds: [{ node: 7, index: 0, weight: 1 }] }
          }
        }
      },
      VRMC_springBone: {
        specVersion: '1.0',
        springs: [{ joints: [{ node: 3 }] }]
      }
    },
    materials: [{
      extensions: {
        VRMC_materials_mtoon: { shadingToonyFactor: 0.9 },
        KHR_materials_unlit: {},
        KHR_texture_transform: { offset: [0, 0], scale: [1, 1] }
      }
    }]
  };

  const json = Buffer.from(JSON.stringify(source), 'utf8');
  const padded = Buffer.alloc(Math.ceil(json.length / 4) * 4, 0x20);
  json.copy(padded);

  const header = Buffer.alloc(12);
  header.write('glTF', 0, 'ascii');
  header.writeUInt32LE(2, 4);
  header.writeUInt32LE(12 + 8 + padded.length, 8);

  const chunkHeader = Buffer.alloc(8);
  chunkHeader.writeUInt32LE(padded.length, 0);
  chunkHeader.writeUInt32LE(0x4E4F534A, 4);
  fs.writeFileSync(input, Buffer.concat([header, chunkHeader, padded]));

  const result = spawnSync(process.execPath, [scriptPath, input, output], { encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr || result.stdout || 'Converter failed.');

  const converted = fs.readFileSync(output);
  assert.equal(converted.toString('ascii', 0, 4), 'glTF');
  const jsonLength = converted.readUInt32LE(12);
  const doc = JSON.parse(converted.subarray(20, 20 + jsonLength).toString('utf8').trim());

  assert.deepEqual(doc.extensionsUsed, ['KHR_materials_unlit', 'KHR_texture_transform']);
  assert.deepEqual(doc.extensionsRequired, ['KHR_materials_unlit']);
  assert.deepEqual(
    Object.keys(doc.materials[0].extensions).sort(),
    ['KHR_materials_unlit', 'KHR_texture_transform']
  );
  assert.equal(doc.extensions, undefined);
  const idle = doc.animations.find(animation => animation.name === 'Ayaka_Idle');
  assert.ok(idle, 'A procedural idle clip must be generated.');
  assert.equal(idle.channels.length, 6, 'Expected arm, torso, neck and head animation channels.');
  const happy = doc.animations.find(animation => animation.name === 'Ayaka_Face_Happy');
  const neutral = doc.animations.find(animation => animation.name === 'Ayaka_Face_Neutral');
  assert.ok(happy?.channels.some(channel => channel.target.path === 'weights'), 'Happy expression morph animation was not generated.');
  assert.ok(neutral?.channels.some(channel => channel.target.path === 'weights'), 'Blinking morph animation was not generated from left/right blink presets.');
  for (const name of [
    'Ayaka_Gesture_Wave', 'Ayaka_Gesture_Pout', 'Ayaka_Gesture_ArmsCrossed', 'Ayaka_Gesture_Dance',
    'Ayaka_Gesture_Nod', 'Ayaka_Gesture_HeadTilt', 'Ayaka_Gesture_Shrug', 'Ayaka_Gesture_Thinking',
    'Ayaka_Gesture_HandOnHeart', 'Ayaka_Gesture_Bow', 'Ayaka_Gesture_Laugh',
    'Ayaka_Gesture_ShyLookAway', 'Ayaka_Gesture_Farewell'
  ]) {
    const gesture = doc.animations.find(animation => animation.name === name);
    assert.ok(gesture?.channels.some(channel => channel.target.path === 'rotation'), name + ' gesture clip was not generated.');
  }
  assert.ok(doc.buffers?.[0]?.byteLength > 0, 'Animation binary data was not appended.');
  assert.ok(doc.accessors?.length >= 10, 'Animation accessors were not created.');

  const metadata = JSON.parse(
    fs.readFileSync(path.join(temp, 'ayaka-vrm-metadata.json'), 'utf8')
  );
  assert.equal(metadata.sourceModel, 'source.vrm');

  const vrm = metadata.extensions.find(
    item => item.name === 'VRMC_vrm' && item.path === 'root'
  ).payload;
  assert.deepEqual(vrm.meta.authors, ['epic_noobster']);
  assert.equal(vrm.meta.creditNotation, 'required');
  assert.equal(vrm.meta.licenseUrl, 'https://example.invalid/vrm-test-license');
  assert.ok(vrm.humanoid.humanBones.head);
  assert.ok(vrm.expressions.preset.happy);

  assert.ok(metadata.extensions.some(item => item.name === 'VRMC_springBone'));
  assert.ok(metadata.extensions.some(
    item => item.name === 'VRMC_materials_mtoon' && item.path === 'root.materials[0]'
  ));

  console.log('Converter regression test passed: KHR extensions preserved; all VRMC metadata retained.');
} finally {
  fs.rmSync(temp, { recursive: true, force: true });
}
