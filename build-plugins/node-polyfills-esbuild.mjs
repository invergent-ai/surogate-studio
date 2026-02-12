/** @type {import('esbuild').Plugin} */
export default {
  name: 'node-polyfills',
  setup(build) {
    const stubs = {
      'crypto': 'export const createHash = () => ({ update: () => ({ digest: () => "" }) }); export const randomBytes = () => ({ toString: () => "" }); export default { createHash, randomBytes };',
      'stream': 'export class Readable {}; export class Writable {}; export class PassThrough {}; export default { Readable, Writable, PassThrough };',
      'stream/promises': 'export const pipeline = () => Promise.resolve(); export default { pipeline };',
      'fs': 'export const promises = { readFile: () => Promise.resolve(""), writeFile: () => Promise.resolve(), readdir: () => Promise.resolve([]), stat: () => Promise.resolve({}), open: () => Promise.resolve({}), realpath: () => Promise.resolve(""), lstat: () => Promise.resolve({}), rename: () => Promise.resolve(), mkdir: () => Promise.resolve() }; export const readFileSync = () => ""; export const writeFileSync = () => {}; export const createWriteStream = () => ({ on: () => ({}), write: () => ({}), end: () => ({}) }); export default { promises, readFileSync, writeFileSync, createWriteStream };',
      'fs/promises': 'export const readFile = () => Promise.resolve(""); export const writeFile = () => Promise.resolve(); export const readdir = () => Promise.resolve([]); export const stat = () => Promise.resolve({}); export const open = () => Promise.resolve({}); export const realpath = () => Promise.resolve(""); export const lstat = () => Promise.resolve({}); export const rename = () => Promise.resolve(); export const mkdir = () => Promise.resolve(); export default { readFile, writeFile, readdir, stat, open, realpath, lstat, rename, mkdir };',
      'os': 'export const platform = () => "browser"; export const release = () => ""; export const homedir = () => "/"; export default { platform, release, homedir };',
      'path': 'export const join = (...args) => args.filter(Boolean).join("/"); export const resolve = (...args) => args.filter(Boolean).join("/"); export const sep = "/"; export const dirname = () => ""; export const basename = () => ""; export const extname = () => ""; export default { join, resolve, sep, dirname, basename, extname };',
      'url': 'export const parse = (url) => ({}); export const format = () => ""; export const resolve = () => ""; export const fileURLToPath = (url) => url; export const pathToFileURL = (path) => path; export class URL { constructor(url, base) { return new window.URL(url, base); } }; export default { parse, format, resolve, fileURLToPath, pathToFileURL, URL };',
    };
    build.onResolve({ filter: /^(node:)?(crypto|stream|fs|os|path|url)(\/.*)?$/ }, args => {
      const moduleName = args.path.replace(/^node:/, '');
      if (stubs[moduleName]) {
        return {
          path: moduleName,
          namespace: 'node-polyfills-stub',
        };
      }
      return null;
    });
    build.onLoad({ filter: /.*/, namespace: 'node-polyfills-stub' }, args => {
      return {
        contents: stubs[args.path] || 'export default {};',
        loader: 'js',
      };
    });
  },
};
