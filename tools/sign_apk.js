#!/usr/bin/env node
// Signing APK (JAR v1 + APK Signature Scheme v2 & v3) memakai apk_sign_ts.
// Usage: node sign_apk.js <in.apk> <out.apk> <keystore.jks> <password>
'use strict';

const { readFileSync, writeFileSync } = require('node:fs');
const path = require('node:path');

async function loadLib() {
  const libDir = process.env.APKSIGN_LIB || path.join(__dirname, '..', 'toolchain', 'apk_sign_ts');
  try {
    return require(libDir);
  } catch (e) {
    return await import(path.join(libDir, 'dist', 'index.js'));
  }
}

(async () => {
  const [inApk, outApk, keystore, password] = process.argv.slice(2);
  if (!inApk || !outApk || !keystore || !password) {
    console.error('Usage: sign_apk.js <in.apk> <out.apk> <keystore.jks> <password>');
    process.exit(1);
  }
  const { ApkSigner, SigningKey, convertToPEM } = await loadLib();
  const keystoreData = new Uint8Array(readFileSync(keystore));
  const { privateKey, certificate } = await convertToPEM(keystoreData, password, 'jks');
  const signer = new ApkSigner({ signingKey: SigningKey.fromPEM(privateKey, certificate) });
  const { signedApk } = await signer.sign(new Uint8Array(readFileSync(inApk)));
  writeFileSync(outApk, signedApk);
  console.log('Signed ->', outApk);
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
