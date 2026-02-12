import path from 'node:path';
import fs from 'node:fs';
/** @type {import('esbuild').Plugin} */
export default {
  name: 'wasm-url',
  setup(build) {
    build.onResolve({ filter: /\.wasm\?url$/ }, async args => {
      const result = await build.resolve(args.path.replace(/\?url$/, ''), {
        resolveDir: args.resolveDir,
        kind: args.kind,
      });
      if (result.errors.length > 0) {
        return { errors: result.errors };
      }
      return {
        path: result.path,
        namespace: 'wasm-url',
      };
    });
    build.onLoad({ filter: /.*/, namespace: 'wasm-url' }, async args => {
      return {
        contents: await fs.promises.readFile(args.path),
        loader: 'file',
      };
    });
  },
};
