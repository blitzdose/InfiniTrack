import { UserConfigFn } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

const customConfig: UserConfigFn = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
    resolve:{
        alias:{
            '@vaadin/vaadin-button/theme/lumo/vaadin-button-styles.js' : '@vaadin/button/theme/lumo/vaadin-button-styles.js'
        },
    },
});

export default overrideVaadinConfig(customConfig);
