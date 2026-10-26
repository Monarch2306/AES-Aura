const registry=new Map();
export function registerMicroFrontend(name,render){registry.set(name,render)}
export function getMicroFrontend(name){return registry.get(name)}
export function listMicroFrontends(){return [...registry.keys()]}
