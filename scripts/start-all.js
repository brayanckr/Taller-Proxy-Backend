import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

const services = [
  { name: 'log-service', dir: 'services/log-service' },
  { name: 'proxy-service', dir: 'services/proxy-service' },
  { name: 'producer-simulator', dir: 'services/producer-simulator', enabled: process.env.SIMULATOR === 'true' },
];

const children = [];

for (const service of services) {
  if (service.enabled === false) continue;
  const child = spawn(process.execPath, ['src/index.js'], {
    cwd: path.join(root, service.dir),
    env: process.env,
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  const prefix = `[${service.name}] `;
  child.stdout.on('data', (chunk) => process.stdout.write(prefix + chunk));
  child.stderr.on('data', (chunk) => process.stderr.write(prefix + chunk));
  child.on('exit', (code) => console.log(`${prefix}exited with code ${code}`));
  children.push(child);
}

const shutdown = () => {
  for (const child of children) child.kill();
  process.exit(0);
};
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
