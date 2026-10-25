'use strict';

const fs = require('node:fs');
const path = require('node:path');

// The repository-root .env is the local source of truth. Reading it here keeps
// the Angular dev proxy target aligned with the backend configuration without
// requiring a second frontend env file.
function readRootEnv() {
  const envPath = path.resolve(__dirname, '..', '.env');
  const values = {};

  try {
    const content = fs.readFileSync(envPath, 'utf8');
    for (const rawLine of content.split(/\r?\n/)) {
      const line = rawLine.trim();
      if (!line || line.startsWith('#')) {
        continue;
      }

      const separator = line.indexOf('=');
      if (separator === -1) {
        continue;
      }

      const key = line.slice(0, separator).trim();
      let value = line.slice(separator + 1).trim();
      if (
        (value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))
      ) {
        value = value.slice(1, -1);
      }
      values[key] = value;
    }
  } catch (error) {
    if (error.code !== 'ENOENT') {
      throw error;
    }
  }

  return values;
}

const env = readRootEnv();
const target = process.env.BACKEND_URL || env.BACKEND_URL || 'http://localhost:8089';

module.exports = {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
    pathRewrite: {
      '^/api': '',
    },
  },
};
