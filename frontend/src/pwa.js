export function registerPWA(){if(import.meta.env.PROD && 'serviceWorker' in navigator){window.addEventListener('load',()=>navigator.serviceWorker.register('/sw.js').catch(()=>{}))}}
