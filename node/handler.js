'use strict';
const { performance } = require('node:perf_hooks');

let isCold = true;

module.exports.hello = async () => {
  const t0 = performance.now();
  const body = {
    message: 'HELLO WORLD',
    runtime: 'nodejs22.x',
    coldStart: isCold,
  };
  isCold = false;
  body.handlerDurationMs = Math.round(performance.now() - t0);

  return {
    statusCode: 200,
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  };
};
