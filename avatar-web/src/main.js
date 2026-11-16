import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { VRMLoaderPlugin, VRMUtils } from '@pixiv/three-vrm';
import './style.css';

const canvas = document.querySelector('#avatar');
const status = document.querySelector('#status');
let renderer;
let vrm = null;
let last = 0;
let blinkAt = 2.5;
let blinkUntil = 0;
let expressionUntil = 0;
let tapped = false;
let modelBaseY = 0;
const clock = new THREE.Clock();

function showStatus(message) {
  status.textContent = message;
  status.style.display = 'grid';
}

function showFailure(message, error) {
  console.error(message, error || '');
  showStatus(message);
}

window.addEventListener('error', event => {
  showFailure('Ayaka display error. Please reopen the app.', event.error || event.message);
});
window.addEventListener('unhandledrejection', event => {
  showFailure('Ayaka could not start. Please reopen the app.', event.reason);
});

try {
  renderer = new THREE.WebGLRenderer({
    canvas, alpha: true, antialias: false, powerPreference: 'low-power',
    preserveDrawingBuffer: false
  });
} catch (error) {
  showFailure('3D graphics are unavailable on this device.', error);
  throw error;
}

renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.35));
renderer.setSize(window.innerWidth, window.innerHeight, false);
renderer.outputColorSpace = THREE.SRGBColorSpace;
renderer.toneMapping = THREE.ACESFilmicToneMapping;
renderer.toneMappingExposure = 1.05;

canvas.addEventListener('webglcontextlost', event => {
  event.preventDefault();
  showFailure('The 3D graphics session stopped. Please reopen the app.');
});

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(28, window.innerWidth / window.innerHeight, 0.01, 100);
scene.add(new THREE.HemisphereLight(0xe6dcff, 0x272039, 2.0));
const key = new THREE.DirectionalLight(0xffe8f3, 2.2);
key.position.set(1.5, 2.5, 3);
scene.add(key);
const fill = new THREE.DirectionalLight(0x9d8aff, 0.8);
fill.position.set(-2, 1, -1);
scene.add(fill);

const loader = new GLTFLoader();
loader.register(parser => new VRMLoaderPlugin(parser));
showStatus('Loading Ayaka…');
loader.load('./ayaka.vrm', gltf => {
  try {
    vrm = gltf.userData.vrm;
    if (!vrm || !vrm.scene) throw new Error('The VRM file did not contain a valid avatar.');

    VRMUtils.removeUnnecessaryVertices(gltf.scene);
    VRMUtils.combineSkeletons(gltf.scene);
    VRMUtils.combineMorphs(vrm);
    scene.add(vrm.scene);
    vrm.scene.traverse(object => { object.frustumCulled = false; });

    // Center the complete model, not just its feet. Some VRM exports have
    // an offset root, which otherwise places the character outside the camera.
    const bounds = new THREE.Box3().setFromObject(vrm.scene);
    const size = bounds.getSize(new THREE.Vector3());
    const center = bounds.getCenter(new THREE.Vector3());
    const height = Math.max(0.1, size.y);
    vrm.scene.position.x -= center.x;
    vrm.scene.position.y -= bounds.min.y;
    vrm.scene.position.z -= center.z;
    modelBaseY = vrm.scene.position.y;

    // Fit the camera to the avatar's measured height and keep the face visible.
    const targetY = height * 0.53;
    const distance = Math.max(1.0, (height * 0.5 / Math.tan(THREE.MathUtils.degToRad(camera.fov * 0.5))) * 1.22);
    camera.position.set(0, targetY + height * 0.025, distance);
    camera.lookAt(0, targetY, 0);
    camera.near = Math.max(0.01, distance / 100);
    camera.far = Math.max(100, distance * 20);
    camera.updateProjectionMatrix();

    console.info('Ayaka model loaded', { height, center: center.toArray(), size: size.toArray() });
  } catch (error) {
    vrm = null;
    showFailure('Ayaka loaded, but could not be displayed.', error);
  }
}, undefined, error => {
  showFailure('Ayaka could not be loaded. Check the app assets.', error);
});

canvas.addEventListener('click', () => {
  if (!vrm?.expressionManager) return;
  tapped = !tapped;
  vrm.expressionManager.setValue('happy', tapped ? 0.85 : 0);
  vrm.expressionManager.setValue('relaxed', tapped ? 0 : 0.25);
  expressionUntil = clock.elapsedTime + 2.2;
});

function resize() {
  const w = Math.max(1, window.innerWidth);
  const h = Math.max(1, window.innerHeight);
  renderer.setSize(w, h, false);
  camera.aspect = w / h;
  camera.updateProjectionMatrix();
}
window.addEventListener('resize', resize, { passive: true });

let firstFrameRendered = false;
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
  if (vrm && !firstFrameRendered) {
    firstFrameRendered = true;
    status.style.display = 'none';
  }
}
requestAnimationFrame(frame);
