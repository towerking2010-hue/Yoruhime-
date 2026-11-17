import * as THREE from 'three';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { VRMLoaderPlugin } from '@pixiv/three-vrm';
import './style.css';

const canvas = document.querySelector('#avatar');
const status = document.querySelector('#status');
let renderer, vrm = null, last = 0, modelBaseY = 0;
let blinkAt = 2.5, blinkUntil = 0, expressionUntil = 0, tapped = false;
let readyReported = false;
const clock = new THREE.Clock();
const debugMarker = new URLSearchParams(window.location.search).get('debug') === '1';

function report(message, error) {
  status.textContent = message;
  status.style.display = message === 'Ayaka is ready' ? 'none' : 'grid';
  try {
    if (window.YoruHimeBridge && typeof window.YoruHimeBridge.onStatus === 'function') {
      window.YoruHimeBridge.onStatus(message);
    }
  } catch (_) { /* Native diagnostics are optional in a desktop preview. */ }
  if (error) console.error(message, error);
}

window.addEventListener('error', e => report('JavaScript error — ' + (e.message || 'check avatar diagnostics'), e.error || e.message));
window.addEventListener('unhandledrejection', e => report('Avatar startup failed — ' + String(e.reason || 'unknown error'), e.reason));

try {
  const gl = canvas.getContext('webgl2', {
    alpha: true,
    antialias: false,
    depth: true,
    stencil: false,
    powerPreference: 'low-power',
    preserveDrawingBuffer: false
  });
  if (!gl) throw new Error('WebGL 2 is unavailable in this Android WebView.');
  renderer = new THREE.WebGLRenderer({
    canvas,
    context: gl,
    alpha: true,
    antialias: false,
    powerPreference: 'low-power'
  });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.25));
  renderer.setSize(Math.max(1, window.innerWidth), Math.max(1, window.innerHeight), false);
  renderer.outputColorSpace = THREE.SRGBColorSpace;
  renderer.setClearColor(0x090b16, 0);
} catch (e) {
  report('3D graphics could not start: ' + (e.message || 'WebGL error'), e);
  throw e;
}

canvas.addEventListener('webglcontextlost', e => {
  e.preventDefault();
  report('Android stopped the 3D graphics session. Reopen YoruHime.');
});
canvas.addEventListener('webglcontextrestored', () => {
  report('3D graphics restored · waking Ayaka…');
});

const scene = new THREE.Scene();
const camera = new THREE.PerspectiveCamera(32, Math.max(1, window.innerWidth) / Math.max(1, window.innerHeight), 0.01, 100);
scene.add(new THREE.HemisphereLight(0xffffff, 0x45405f, 2.0));
const key = new THREE.DirectionalLight(0xffffff, 2.2);
key.position.set(1.5, 2.5, 3);
scene.add(key);
const fill = new THREE.DirectionalLight(0xb8a5ff, 0.9);
fill.position.set(-2, 1, -1);
scene.add(fill);

// Only shown when the user deliberately opens the local diagnostic URL with ?debug=1.
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

    scene.add(vrm.scene);
    vrm.scene.traverse(object => {
      if (object.isMesh) object.frustumCulled = false;
    });

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
    marker.visible = debugMarker;

    // Frame by the actual avatar bounds so different VRM heights remain usable.
    const distance = Math.max(
      1.15,
      (height * 0.5 / Math.tan(THREE.MathUtils.degToRad(camera.fov * 0.5))) * 1.16
    );
    camera.position.set(0, targetY + height * 0.025, distance);
    camera.lookAt(0, targetY, 0);
    camera.near = 0.01;
    camera.far = 100;
    camera.updateProjectionMatrix();

    report('Ayaka model loaded · preparing her first frame…');
    console.info('VRM diagnostics', {
      height,
      center: center.toArray(),
      size: size.toArray(),
      meshCount: (() => {
        let count = 0;
        vrm.scene.traverse(object => { if (object.isMesh) count++; });
        return count;
      })(),
      camera: camera.position.toArray(),
      webgl: renderer.getContext().getParameter(renderer.getContext().VERSION)
    });
  } catch (e) {
    vrm = null;
    report('Ayaka model loaded, but her 3D scene failed validation: ' + (e.message || 'unknown error'), e);
  }
}, undefined, error => {
  report('Could not load ayaka.vrm — ' + (error?.message || 'bundled model missing'), error);
});

canvas.addEventListener('click', () => {
  if (!vrm?.expressionManager) return;
  tapped = !tapped;
  vrm.expressionManager.setValue('happy', tapped ? 0.8 : 0);
  expressionUntil = clock.elapsedTime + 2.2;
});

function resize() {
  const width = Math.max(1, window.innerWidth);
  const height = Math.max(1, window.innerHeight);
  renderer.setSize(width, height, false);
  camera.aspect = width / height;
  camera.updateProjectionMatrix();
}
window.addEventListener('resize', resize, { passive: true });

function frame(now) {
  requestAnimationFrame(frame);
  if (now - last < 33) return;
  last = now;
  const delta = Math.min(clock.getDelta(), 0.05);

  if (vrm) {
    const time = clock.elapsedTime;
    vrm.scene.position.y = modelBaseY + Math.sin(time * 1.1) * 0.004;
    vrm.scene.rotation.z = Math.sin(time * 0.55) * 0.012;

    if (vrm.expressionManager) {
      if (time > blinkAt) {
        blinkUntil = time + 0.13;
        blinkAt = time + 3 + Math.random() * 2.5;
      }
      vrm.expressionManager.setValue('blink', time < blinkUntil ? 1 : 0);
      if (time > expressionUntil && tapped) {
        tapped = false;
        vrm.expressionManager.setValue('happy', 0);
      }
    }
    vrm.update(delta);
  }

  renderer.render(scene, camera);
  if (vrm && !readyReported) {
    readyReported = true;
    report('Ayaka is ready');
  }
}
requestAnimationFrame(frame);
