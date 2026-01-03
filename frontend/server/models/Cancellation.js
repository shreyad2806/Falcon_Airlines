import mongoose from 'mongoose'
const Schema = mongoose.Schema

const CancellationSchema = new Schema({
  ticketId: String,
  flightCode: String,
  cancellationDate: String
},{ timestamps: true })

export default mongoose.model('Cancellation', CancellationSchema)
