import client from './client';

export const generateBoardingPass = (ticketId) => client.post(`/api/boarding-passes/ticket/${ticketId}`);
export const getBoardingPass = (id) => client.get(`/api/boarding-passes/${id}`);
export const getBoardingPassByNumber = (num) => client.get(`/api/boarding-passes/number/${num}`);
export const getBoardingPassesByBooking = (bookingId) => client.get(`/api/boarding-passes/booking/${bookingId}`);
export const updateStatus = (id, status) => client.put(`/api/boarding-passes/${id}/status`, null, { params: { status } });
export const checkIn = (id) => client.post(`/api/boarding-passes/${id}/check-in`);
export const boardPassenger = (id) => client.post(`/api/boarding-passes/${id}/board`);
export const getQrCode = (id) => client.get(`/api/boarding-passes/${id}/qr-code`);

export const downloadBoardingPassPdf = async (id) => {
  const res = await client.get(`/api/boarding-passes/${id}/pdf`, { responseType: 'blob' });
  return res.data;
};
