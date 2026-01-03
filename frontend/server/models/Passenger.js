import mongoose from 'mongoose'
const Schema = mongoose.Schema

const PassengerSchema = new Schema({
  passengerName: String,
  nationality: String,
  gender: String,
  passportNumber: String,
  phoneNumber: String,
  address: String
},{ timestamps: true })

export default mongoose.model('Passenger', PassengerSchema)
