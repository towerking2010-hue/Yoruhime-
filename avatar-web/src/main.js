import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { VRMLoaderPlugin, VRMUtils } from '@pixiv/three-vrm';
import './style.css';

const canvas = document.querySelector('#avatar');
const status = document.querySelector('#status');
const renderer = new THREE.WebGLRenderer({
  canvas, alpha: true, antialias: false, powerPreference: 'low-power',
  preserveDrawingBuffer: false
});
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.35));
renderer.setSize(window.innerWidth, window.innerHeight, false);
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.05;

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(28, window.innerWidth / window.innerHeight, 0.1, 100);
camera.position.set(0, 1.25, 3.2);
scene.add(new THREE.HemisphereLight(0xe6dcff, 0x272039, 2.0));
const key = new THREE.DirectionalLight(0xffe8f3, 2.2);
key.position.set(1.5, 2.5, 3);
scene.add(key);
const fill = new THREE.DirectionalLight(0x9d8aff, 0.8);
fill.position.set(-2, 1, -1);
scene.add(fill);

let vrm = null;
let last = 0;
let blinkAt = 2.5;
let blinkUntil = 0;
let expressionUntil = 0;
let tapped = false;
let modelBaseY = 0;
const clock = new THREE.Clock();

const loader = new GLTFLoader();
loader.register(parser => new VRMLoaderPlugin(parser));
loader.load('./ayaka.vrm', gltf => {
  vrm = gltf.userData.vrm;
  VRMUtils.removeUnnecessaryVertices(gltf.scene);
  VRMUtils.removeUnnecessaryJoints(gltf.scene);
  scene.add(vrm.scene);
  vrm.scene.rotation.y = Math.PI;
  const bounds = new THREE.Box3().setFromObject(vrm.scene);
  const height = Math.max(1, bounds.max.y - bounds.min.y);
  const center = bounds.getCenter(new THREE.Vector3());
  vrm.scene.position.y -= bounds.min.y;
  modelBaseY = vrm.scene.position.y;
  camera.position.set(0, height * 0.59, height * 2.55);
  camera.lookAt(0, height * 0.52, 0);
  camera.near = height / 100;
  camera.far = height * 20;
  camera.updateProjectionMatrix();
  status.style.display = 'none';
  canvas.addEventListener('click', () => {
    if (!vrm?.expressionManager) return;
    tapped = !tapped;
    vrm.expressionManager.setValue('happy', tapped ? 0.85 : 0);
    vrm.expressionManager.setValue('relaxed', tapped ? 0 : 0.25);
    expressionUntil = clock.elapsedTime + 2.2;
  });
}, undefined, error => {
  console.error('Ayaka VRM failed to load', error);
  status.textContent = 'Ayaka could not be loaded';
});

function resize() {
  const w = window.innerWidth, h = window.innerHeight;
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
      if (t > blinkAt) {
        blinkUntil = t + 0.13;
        blinkAt = t + 3.0 + Math.random() * 2.5;
      }
      vrm.expressionManager.setValue('blink', t < blinkUntil ? 1 : 0);
      if (t > expressionUntil && tapped) {
        tapped = false;
        vrm.expressionManager.setValue('happy', 0);
        vrm.expressionManager.setValue('relaxed', 0.25);
      }
    }
    vrm.update(delta);
  }
  renderer.render(scene, camera);
}
requestAnimationFrame(frame);

