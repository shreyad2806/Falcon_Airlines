import client from './client';

export const getTicket = (id) => client.get(`/api/tickets/${id}`);
export const getTicketByNumber = (num) => client.get(`/api/tickets/number/${num}`);
export const getTicketsByBooking = (bookingId) => client.get(`/api/tickets/booking/${bookingId}`);
export const cancelTicket = (id) => client.post(`/api/tickets/${id}/cancel`);

export const downloadTicketPdf = async (id) => {
  const res = await client.get(`/api/tickets/${id}/pdf`, { responseType: 'blob' });
  return res.data;
};
