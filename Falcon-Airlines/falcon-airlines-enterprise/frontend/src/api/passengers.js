import client from './client';

export const searchPassengers = (params = {}) => client.get('/api/passengers', { params });
export const getPassenger = (id) => client.get(`/api/passengers/${id}`);
export const getMyPassenger = () => client.get('/api/passengers/me');
export const createPassenger = (data) => client.post('/api/passengers', data);
export const updatePassenger = (id, data) => client.put(`/api/passengers/${id}`, data);
