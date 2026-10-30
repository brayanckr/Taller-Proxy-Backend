import { createApp } from './app.js';

const port = Number(process.env.PORT) || 4001;
const internalToken = process.env.INTERNAL_TOKEN || 'internal-secret';

createApp({ internalToken }).listen(port, () => {
  console.log(`log-service listening on port ${port}`);
});
