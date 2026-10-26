import api from '../api/client'
export async function authenticatedImageUrl(path){const token=localStorage.getItem('aes_token');const r=await fetch(`${api.defaults.baseURL}${path}`,{headers:{Authorization:`Bearer ${token}`}});if(!r.ok)throw new Error('Unable to load protected file');const blob=await r.blob();return URL.createObjectURL(blob)}
