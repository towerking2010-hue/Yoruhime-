import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { VRMLoaderPlugin } from '@pixiv/three-vrm';
import './style.css';

const canvas = document.querySelector('#avatar');
const status = document.querySelector('#status');
let renderer, vrm = null, last = 0, modelBaseY = 0;
let blinkAt = 2.5, blinkUntil = 0, expressionUntil = 0, tapped = false;
const clock = new THREE.Clock();

function report(message, error) {
  status.textContent = message;
  status.style.display = 'grid';
  if (error) console.error(message, error);
}
window.addEventListener('error', e => report('JavaScript error — check avatar diagnostics', e.error || e.message));
window.addEventListener('unhandledrejection', e => report('Avatar startup failed — check diagnostics', e.reason));

try {
  renderer = new THREE.WebGLRenderer({ canvas, alpha: true, antialias: false, powerPreference: 'low-power' });
} catch (e) {
  report('This phone could not start WebGL graphics.', e);
  throw e;
}
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.35));
renderer.setSize(Math.max(1, window.innerWidth), Math.max(1, window.innerHeight), false);
renderer.outputColorSpace = THREE.SRGBColorSpace;

canvas.addEventListener('webglcontextlost', e => {
  e.preventDefault();
  report('Android stopped the 3D graphics session. Reopen YoruHime.');
});
const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(32, Math.max(1, window.innerWidth) / Math.max(1, window.innerHeight), 0.01, 100);
scene.add(new THREE.HemisphereLight(0xffffff, 0x45405f, 2.2));
const key = new THREE.DirectionalLight(0xffffff, 2.4);
key.position.set(1.5, 2.5, 3);
scene.add(key);
const fill = new THREE.DirectionalLight(0xb8a5ff, 1.0);
fill.position.set(-2, 1, -1);
scene.add(fill);

// Temporary, unlit marker: if this appears, WebView/WebGL drawing works and
// the remaining fault is specific to the VRM scene, materials, or skeleton.
const marker = new THREE.Mesh(
  new THREE.SphereGeometry(0.13, 16, 12),
  new THREE.MeshBasicMaterial({ color: 0xff35c9 })
);
marker.visible = false;
scene.add(marker);

const loader = new GLTFLoader();
loader.register(parser => new VRMLoaderPlugin(parser));
report('3D renderer started · loading Ayaka…');
loader.load('./ayaka.vrm', gltf => {
  try {
    vrm = gltf.userData.vrm;
    if (!vrm?.scene) throw new Error('VRM loader returned no avatar scene.');

    // Keep the original VRM geometry and skeleton untouched while diagnosing.
    scene.add(vrm.scene);
    vrm.scene.traverse(o => { if (o.isMesh) o.frustumCulled = false; });

    vrm.scene.updateMatrixWorld(true);
    const bounds = new THREE.Box3().setFromObject(vrm.scene);
    const size = bounds.getSize(new THREE.Vector3());
    const center = bounds.getCenter(new THREE.Vector3());
    const height = size.y;
    if (!Number.isFinite(height) || height < 0.2 || !Number.isFinite(center.x + center.y + center.z)) {
      throw new Error('Avatar has invalid 3D bounds: ' + JSON.stringify({ min: bounds.min.toArray(), max: bounds.max.toArray() }));
    }

    vrm.scene.position.x -= center.x;
    vrm.scene.position.y -= bounds.min.y;
    vrm.scene.position.z -= center.z;
    modelBaseY = vrm.scene.position.y;
    const targetY = height * 0.53;
    marker.position.set(0, targetY, 0.18);
    marker.visible = true;

    const distance = Math.max(1.2, (height * 0.5 / Math.tan(THREE.MathUtils.degToRad(camera.fov * 0.5))) * 1.3);
    camera.position.set(0, targetY + height * 0.025, distance);
    camera.lookAt(0, targetY, 0);
    camera.near = 0.01;
    camera.far = 100;
    camera.updateProjectionMatrix();

    report('Ayaka file loaded · pink marker is a graphics test');
    console.info('VRM diagnostics', {
      height, center: center.toArray(), size: size.toArray(),
      meshCount: (() => { let n = 0; vrm.scene.traverse(o => { if (o.isMesh) n++; }); return n; })(),
      camera: camera.position.toArray()
    });
  } catch (e) {
    vrm = null;
    report('Ayaka model data loaded, but its 3D scene failed validation.', e);
  }
}, undefined, e => report('Could not load ayaka.vrm — check bundled assets.', e));

canvas.addEventListener('click', () => {
  if (!vrm?.expressionManager) return;
  tapped = !tapped;
  vrm.expressionManager.setValue('happy', tapped ? 0.8 : 0);
  expressionUntil = clock.elapsedTime + 2.2;
});

function resize() {
  const w = Math.max(1, window.innerWidth), h = Math.max(1, window.innerHeight);
  renderer.setSize(w, h, false);
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
}
window.addEventListener('resize', resize, { passive: true });

function frame(now) {
  requestAnimationFrame(frame);
  if (now - last < 33) return;
  last = now;
  const delta = Math.min(clock.getDelta(), 0.05);
  if (vrm) {
    const t = clock.elapsedTime;
    vrm.scene.position.y = modelBaseY + Math.sin(t * 1.1) * 0.004;
    vrm.scene.rotation.z = Math.sin(t * 0.55) * 0.012;
    if (vrm.expressionManager) {
      if (t > blinkAt) { blinkUntil = t + 0.13; blinkAt = t + 3 + Math.random() * 2.5; }
      vrm.expressionManager.setValue('blink', t < blinkUntil ? 1 : 0);
      if (t > expressionUntil && tapped) {
        tapped = false;
        vrm.expressionManager.setValue('happy', 0);
      }
    }
    vrm.update(delta);
  }
  renderer.render(scene, camera);
}
requestAnimationFrame(frame);
