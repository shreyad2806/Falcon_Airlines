import client from './client';

export const register = (data) => client.post('/auth/register', data);
export const login = (data) => client.post('/auth/login', data);
export const refreshToken = (refreshToken) => client.post('/auth/refresh', { refreshToken });
export const logout = (refreshToken) => client.post('/auth/logout', { refreshToken });
