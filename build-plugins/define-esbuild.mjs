/** @type {import('esbuild').Plugin} */
export default {
    name: 'define:vars',
    setup(build) {
        build.initialOptions.define ??= {};
        Object.assign(build.initialOptions.define, {
            __VERSION__: JSON.stringify(process.env.APP_VERSION ?? 'unknown'),
            __DEBUG_INFO_ENABLED__: JSON.stringify(process.env.APP_VERSION ?? 'unknown')
        });
    }
};
