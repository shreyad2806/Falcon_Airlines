// Global variables
let flights = [];
let passengers = [];
let bookings = [];
let cancellations = [];
let editingItem = null;

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    try {
        initializeNavigation();
        loadDashboardStats();
        loadAllData();
        setupFormHandlers();
        setupModalHandlers();
    } catch (error) {
        console.error('Initialization error:', error);
        showMessage('Application initialization failed. Please refresh the page.', 'error');
    }
});

// Navigation functionality
function initializeNavigation() {
    try {
        const hamburger = document.querySelector('.hamburger');
        const navMenu = document.querySelector('.nav-menu');
        const navLinks = document.querySelectorAll('.nav-link');

        if (hamburger && navMenu) {
            hamburger.addEventListener('click', () => {
                hamburger.classList.toggle('active');
                navMenu.classList.toggle('active');
            });
        }

        navLinks.forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                const targetId = link.getAttribute('href').substring(1);
                showSection(targetId);
                
                // Update active nav link
                navLinks.forEach(l => l.classList.remove('active'));
                link.classList.add('active');
                
                // Close mobile menu
                if (hamburger) hamburger.classList.remove('active');
                if (navMenu) navMenu.classList.remove('active');
            });
        });
    } catch (error) {
        console.error('Navigation initialization error:', error);
    }
}

// Show different sections
function showSection(sectionId) {
    try {
        const sections = document.querySelectorAll('.section');
        sections.forEach(section => {
            section.classList.remove('active');
        });
        
        const targetSection = document.getElementById(sectionId);
        if (targetSection) {
            targetSection.classList.add('active');
        }
    } catch (error) {
        console.error('Section display error:', error);
    }
}

// Load dashboard statistics
function loadDashboardStats() {
    try {
        const totalFlights = document.getElementById('totalFlights');
        const totalPassengers = document.getElementById('totalPassengers');
        const totalBookings = document.getElementById('totalBookings');
        const totalCancellations = document.getElementById('totalCancellations');

        if (totalFlights) totalFlights.textContent = flights.length;
        if (totalPassengers) totalPassengers.textContent = passengers.length;
        if (totalBookings) totalBookings.textContent = bookings.length;
        if (totalCancellations) totalCancellations.textContent = cancellations.length;
    } catch (error) {
        console.error('Dashboard stats error:', error);
    }
}

// Load all data from localStorage
function loadAllData() {
    try {
        flights = JSON.parse(localStorage.getItem('flights') || '[]');
        passengers = JSON.parse(localStorage.getItem('passengers') || '[]');
        bookings = JSON.parse(localStorage.getItem('bookings') || '[]');
        cancellations = JSON.parse(localStorage.getItem('cancellations') || '[]');
        
        // Validate data structure
        if (!Array.isArray(flights)) flights = [];
        if (!Array.isArray(passengers)) passengers = [];
        if (!Array.isArray(bookings)) bookings = [];
        if (!Array.isArray(cancellations)) cancellations = [];
        
        displayFlights();
        displayPassengers();
        displayBookings();
        displayCancellations();
        updateDropdowns();
        loadDashboardStats();
    } catch (error) {
        console.error('Data loading error:', error);
        // Reset data if corrupted
        flights = [];
        passengers = [];
        bookings = [];
        cancellations = [];
        saveData();
    }
}

// Save data to localStorage
function saveData() {
    try {
        localStorage.setItem('flights', JSON.stringify(flights));
        localStorage.setItem('passengers', JSON.stringify(passengers));
        localStorage.setItem('bookings', JSON.stringify(bookings));
        localStorage.setItem('cancellations', JSON.stringify(cancellations));
    } catch (error) {
        console.error('Data saving error:', error);
        showMessage('Failed to save data. Please check your browser settings.', 'error');
    }
}

// Setup form handlers
function setupFormHandlers() {
    try {
        // Flight form
        const flightForm = document.getElementById('flightForm');
        if (flightForm) {
            flightForm.addEventListener('submit', handleFlightSubmit);
        }
        
        // Passenger form
        const passengerForm = document.getElementById('passengerForm');
        if (passengerForm) {
            passengerForm.addEventListener('submit', handlePassengerSubmit);
        }
        
        // Booking form
        const bookingForm = document.getElementById('bookingForm');
        if (bookingForm) {
            bookingForm.addEventListener('submit', handleBookingSubmit);
        }
        
        // Cancellation form
        const cancellationForm = document.getElementById('cancellationForm');
        if (cancellationForm) {
            cancellationForm.addEventListener('submit', handleCancellationSubmit);
        }
        
        // Booking passenger selection
        const bookingPassengerId = document.getElementById('bookingPassengerId');
        if (bookingPassengerId) {
            bookingPassengerId.addEventListener('change', handlePassengerSelection);
        }
        
        // Cancellation ticket selection
        const cancellationTicketId = document.getElementById('cancellationTicketId');
        if (cancellationTicketId) {
            cancellationTicketId.addEventListener('change', handleTicketSelection);
        }
    } catch (error) {
        console.error('Form handler setup error:', error);
    }
}

// Flight form handlers
function handleFlightSubmit(e) {
    e.preventDefault();
    try {
        const formData = new FormData(e.target);
        const flightData = {
            id: editingItem ? editingItem.id : Date.now(),
            flightCode: formData.get('flightCode')?.trim() || '',
            source: formData.get('source') || '',
            destination: formData.get('destination') || '',
            date: formData.get('flightDate') || '',
            seats: parseInt(formData.get('seats')) || 0
        };
        
        // Validation
        if (!flightData.flightCode || !flightData.source || !flightData.destination || !flightData.date || flightData.seats <= 0) {
            showMessage('Please fill in all required fields correctly.', 'error');
            return;
        }
        
        if (editingItem) {
            const index = flights.findIndex(f => f.id === editingItem.id);
            if (index !== -1) {
                flights[index] = flightData;
                showMessage('Flight updated successfully!', 'success');
            }
        } else {
            flights.push(flightData);
            showMessage('Flight added successfully!', 'success');
        }
        
        saveData();
        displayFlights();
        updateDropdowns();
        loadDashboardStats();
        clearFlightForm();
        editingItem = null;
    } catch (error) {
        console.error('Flight submission error:', error);
        showMessage('Failed to save flight. Please try again.', 'error');
    }
}

function clearFlightForm() {
    try {
        const form = document.getElementById('flightForm');
        if (form) form.reset();
        editingItem = null;
    } catch (error) {
        console.error('Clear flight form error:', error);
    }
}

// Passenger form handlers
function handlePassengerSubmit(e) {
    e.preventDefault();
    try {
        const formData = new FormData(e.target);
        const passengerData = {
            id: editingItem ? editingItem.id : passengers.length + 1,
            name: formData.get('passengerName')?.trim() || '',
            nationality: formData.get('nationality') || '',
            gender: formData.get('gender') || '',
            passport: formData.get('passportNumber')?.trim() || '',
            phone: formData.get('phoneNumber')?.trim() || '',
            address: formData.get('address')?.trim() || ''
        };
        
        // Validation
        if (!passengerData.name || !passengerData.nationality || !passengerData.gender || !passengerData.passport || !passengerData.phone || !passengerData.address) {
            showMessage('Please fill in all required fields.', 'error');
            return;
        }
        
        if (editingItem) {
            const index = passengers.findIndex(p => p.id === editingItem.id);
            if (index !== -1) {
                passengers[index] = passengerData;
                showMessage('Passenger updated successfully!', 'success');
            }
        } else {
            passengers.push(passengerData);
            showMessage('Passenger added successfully!', 'success');
        }
        
        saveData();
        displayPassengers();
        updateDropdowns();
        loadDashboardStats();
        clearPassengerForm();
        editingItem = null;
    } catch (error) {
        console.error('Passenger submission error:', error);
        showMessage('Failed to save passenger. Please try again.', 'error');
    }
}

function clearPassengerForm() {
    try {
        const form = document.getElementById('passengerForm');
        if (form) form.reset();
        editingItem = null;
    } catch (error) {
        console.error('Clear passenger form error:', error);
    }
}

// Booking form handlers
function handleBookingSubmit(e) {
    e.preventDefault();
    try {
        const formData = new FormData(e.target);
        const bookingData = {
            id: bookings.length + 1,
            passengerId: parseInt(formData.get('bookingPassengerId')) || 0,
            passengerName: formData.get('bookingPassengerName')?.trim() || '',
            flightCode: formData.get('bookingFlightCode') || '',
            gender: formData.get('bookingGender')?.trim() || '',
            passport: formData.get('bookingPassport')?.trim() || '',
            nationality: formData.get('bookingNationality')?.trim() || '',
            amount: parseInt(formData.get('bookingAmount')) || 0
        };
        
        // Validation
        if (!bookingData.passengerId || !bookingData.passengerName || !bookingData.flightCode || !bookingData.amount || bookingData.amount <= 0) {
            showMessage('Please fill in all required fields correctly.', 'error');
            return;
        }
        
        bookings.push(bookingData);
        saveData();
        displayBookings();
        updateDropdowns();
        loadDashboardStats();
        clearBookingForm();
        showMessage('Ticket booked successfully!', 'success');
    } catch (error) {
        console.error('Booking submission error:', error);
        showMessage('Failed to book ticket. Please try again.', 'error');
    }
}

function clearBookingForm() {
    try {
        const form = document.getElementById('bookingForm');
        if (form) form.reset();
        
        const bookingPassengerName = document.getElementById('bookingPassengerName');
        const bookingGender = document.getElementById('bookingGender');
        const bookingPassport = document.getElementById('bookingPassport');
        const bookingNationality = document.getElementById('bookingNationality');
        
        if (bookingPassengerName) bookingPassengerName.value = '';
        if (bookingGender) bookingGender.value = '';
        if (bookingPassport) bookingPassport.value = '';
        if (bookingNationality) bookingNationality.value = '';
    } catch (error) {
        console.error('Clear booking form error:', error);
    }
}

function handlePassengerSelection() {
    try {
        const passengerId = document.getElementById('bookingPassengerId')?.value;
        const passenger = passengers.find(p => p.id == passengerId);
        
        if (passenger) {
            const bookingPassengerName = document.getElementById('bookingPassengerName');
            const bookingGender = document.getElementById('bookingGender');
            const bookingPassport = document.getElementById('bookingPassport');
            const bookingNationality = document.getElementById('bookingNationality');
            
            if (bookingPassengerName) bookingPassengerName.value = passenger.name;
            if (bookingGender) bookingGender.value = passenger.gender;
            if (bookingPassport) bookingPassport.value = passenger.passport;
            if (bookingNationality) bookingNationality.value = passenger.nationality;
        }
    } catch (error) {
        console.error('Passenger selection error:', error);
    }
}

// Cancellation form handlers
function handleCancellationSubmit(e) {
    e.preventDefault();
    try {
        const formData = new FormData(e.target);
        const cancellationData = {
            id: cancellations.length + 1,
            ticketId: parseInt(formData.get('cancellationTicketId')) || 0,
            flightCode: formData.get('cancellationFlightCode')?.trim() || '',
            date: formData.get('cancellationDate') || ''
        };
        
        // Validation
        if (!cancellationData.ticketId || !cancellationData.flightCode || !cancellationData.date) {
            showMessage('Please fill in all required fields.', 'error');
            return;
        }
        
        // Remove the booking
        const ticketId = parseInt(formData.get('cancellationTicketId'));
        bookings = bookings.filter(b => b.id !== ticketId);
        
        cancellations.push(cancellationData);
        saveData();
        displayCancellations();
        displayBookings();
        updateDropdowns();
        loadDashboardStats();
        clearCancellationForm();
        showMessage('Ticket cancelled successfully!', 'success');
    } catch (error) {
        console.error('Cancellation submission error:', error);
        showMessage('Failed to cancel ticket. Please try again.', 'error');
    }
}

function clearCancellationForm() {
    try {
        const form = document.getElementById('cancellationForm');
        if (form) form.reset();
        
        const cancellationFlightCode = document.getElementById('cancellationFlightCode');
        if (cancellationFlightCode) cancellationFlightCode.value = '';
    } catch (error) {
        console.error('Clear cancellation form error:', error);
    }
}

function handleTicketSelection() {
    try {
        const ticketId = document.getElementById('cancellationTicketId')?.value;
        const booking = bookings.find(b => b.id == ticketId);
        
        if (booking) {
            const cancellationFlightCode = document.getElementById('cancellationFlightCode');
            if (cancellationFlightCode) cancellationFlightCode.value = booking.flightCode;
        }
    } catch (error) {
        console.error('Ticket selection error:', error);
    }
}

// Display functions
function displayFlights() {
    try {
        const tbody = document.getElementById('flightsTableBody');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        
        flights.forEach(flight => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${escapeHtml(flight.flightCode)}</td>
                <td>${escapeHtml(flight.source)}</td>
                <td>${escapeHtml(flight.destination)}</td>
                <td>${escapeHtml(flight.date)}</td>
                <td>${flight.seats}</td>
                <td>
                    <div class="action-buttons-table">
                        <button class="btn-edit" onclick="editFlight(${flight.id})">Edit</button>
                        <button class="btn-delete" onclick="deleteFlight(${flight.id})">Delete</button>
                    </div>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Display flights error:', error);
    }
}

function displayPassengers() {
    try {
        const tbody = document.getElementById('passengersTableBody');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        
        passengers.forEach(passenger => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${passenger.id}</td>
                <td>${escapeHtml(passenger.name)}</td>
                <td>${escapeHtml(passenger.nationality)}</td>
                <td>${escapeHtml(passenger.gender)}</td>
                <td>${escapeHtml(passenger.passport)}</td>
                <td>${escapeHtml(passenger.phone)}</td>
                <td>${escapeHtml(passenger.address)}</td>
                <td>
                    <div class="action-buttons-table">
                        <button class="btn-edit" onclick="editPassenger(${passenger.id})">Edit</button>
                        <button class="btn-delete" onclick="deletePassenger(${passenger.id})">Delete</button>
                    </div>
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Display passengers error:', error);
    }
}

function displayBookings() {
    try {
        const tbody = document.getElementById('bookingsTableBody');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        
        bookings.forEach(booking => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${booking.id}</td>
                <td>${escapeHtml(booking.passengerName)}</td>
                <td>${escapeHtml(booking.flightCode)}</td>
                <td>${escapeHtml(booking.gender)}</td>
                <td>${escapeHtml(booking.passport)}</td>
                <td>$${booking.amount}</td>
                <td>${escapeHtml(booking.nationality)}</td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Display bookings error:', error);
    }
}

function displayCancellations() {
    try {
        const tbody = document.getElementById('cancellationsTableBody');
        if (!tbody) return;
        
        tbody.innerHTML = '';
        
        cancellations.forEach(cancellation => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${cancellation.id}</td>
                <td>${cancellation.ticketId}</td>
                <td>${escapeHtml(cancellation.flightCode)}</td>
                <td>${escapeHtml(cancellation.date)}</td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Display cancellations error:', error);
    }
}

// Update dropdowns
function updateDropdowns() {
    try {
        // Update passenger dropdown for bookings
        const passengerSelect = document.getElementById('bookingPassengerId');
        if (passengerSelect) {
            passengerSelect.innerHTML = '<option value="">Select Passenger</option>';
            passengers.forEach(passenger => {
                const option = document.createElement('option');
                option.value = passenger.id;
                option.textContent = `${passenger.id} - ${escapeHtml(passenger.name)}`;
                passengerSelect.appendChild(option);
            });
        }
        
        // Update flight dropdown for bookings
        const flightSelect = document.getElementById('bookingFlightCode');
        if (flightSelect) {
            flightSelect.innerHTML = '<option value="">Select Flight</option>';
            flights.forEach(flight => {
                const option = document.createElement('option');
                option.value = flight.flightCode;
                option.textContent = `${flight.flightCode} (${flight.source} to ${flight.destination})`;
                flightSelect.appendChild(option);
            });
        }
        
        // Update ticket dropdown for cancellations
        const ticketSelect = document.getElementById('cancellationTicketId');
        if (ticketSelect) {
            ticketSelect.innerHTML = '<option value="">Select Ticket</option>';
            bookings.forEach(booking => {
                const option = document.createElement('option');
                option.value = booking.id;
                option.textContent = `Ticket ${booking.id} - ${escapeHtml(booking.passengerName)}`;
                ticketSelect.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Update dropdowns error:', error);
    }
}

// Edit functions
function editFlight(id) {
    try {
        const flight = flights.find(f => f.id === id);
        if (flight) {
            editingItem = flight;
            const flightCode = document.getElementById('flightCode');
            const source = document.getElementById('source');
            const destination = document.getElementById('destination');
            const flightDate = document.getElementById('flightDate');
            const seats = document.getElementById('seats');
            
            if (flightCode) flightCode.value = flight.flightCode;
            if (source) source.value = flight.source;
            if (destination) destination.value = flight.destination;
            if (flightDate) flightDate.value = flight.date;
            if (seats) seats.value = flight.seats;
        }
    } catch (error) {
        console.error('Edit flight error:', error);
    }
}

function editPassenger(id) {
    try {
        const passenger = passengers.find(p => p.id === id);
        if (passenger) {
            editingItem = passenger;
            const passengerName = document.getElementById('passengerName');
            const nationality = document.getElementById('nationality');
            const gender = document.getElementById('gender');
            const passportNumber = document.getElementById('passportNumber');
            const phoneNumber = document.getElementById('phoneNumber');
            const address = document.getElementById('address');
            
            if (passengerName) passengerName.value = passenger.name;
            if (nationality) nationality.value = passenger.nationality;
            if (gender) gender.value = passenger.gender;
            if (passportNumber) passportNumber.value = passenger.passport;
            if (phoneNumber) phoneNumber.value = passenger.phone;
            if (address) address.value = passenger.address;
        }
    } catch (error) {
        console.error('Edit passenger error:', error);
    }
}

// Delete functions
function deleteFlight(id) {
    try {
        if (confirm('Are you sure you want to delete this flight?')) {
            flights = flights.filter(f => f.id !== id);
            saveData();
            displayFlights();
            updateDropdowns();
            loadDashboardStats();
            showMessage('Flight deleted successfully!', 'success');
        }
    } catch (error) {
        console.error('Delete flight error:', error);
        showMessage('Failed to delete flight. Please try again.', 'error');
    }
}

function deletePassenger(id) {
    try {
        if (confirm('Are you sure you want to delete this passenger?')) {
            passengers = passengers.filter(p => p.id !== id);
            saveData();
            displayPassengers();
            updateDropdowns();
            loadDashboardStats();
            showMessage('Passenger deleted successfully!', 'success');
        }
    } catch (error) {
        console.error('Delete passenger error:', error);
        showMessage('Failed to delete passenger. Please try again.', 'error');
    }
}

// Modal functionality
function setupModalHandlers() {
    try {
        const modal = document.getElementById('editModal');
        const closeBtn = document.querySelector('.close');
        
        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                if (modal) modal.style.display = 'none';
            });
        }
        
        if (modal) {
            window.addEventListener('click', (e) => {
                if (e.target === modal) {
                    modal.style.display = 'none';
                }
            });
        }
    } catch (error) {
        console.error('Modal setup error:', error);
    }
}

// Message system
function showMessage(message, type = 'success') {
    try {
        const container = document.getElementById('messageContainer');
        if (!container) return;
        
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.textContent = message;
        
        container.appendChild(messageDiv);
        
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.remove();
            }
        }, 3000);
    } catch (error) {
        console.error('Show message error:', error);
    }
}

// Utility function to escape HTML
function escapeHtml(text) {
    if (typeof text !== 'string') return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Add some sample data for demonstration
function addSampleData() {
    try {
        if (flights.length === 0) {
            flights = [
                { id: 1, flightCode: 'FA001', source: 'Delhi', destination: 'Mumbai', date: '2024-01-15', seats: 150 },
                { id: 2, flightCode: 'FA002', source: 'Mumbai', destination: 'Bangalore', date: '2024-01-16', seats: 120 },
                { id: 3, flightCode: 'FA003', source: 'Bangalore', destination: 'Delhi', date: '2024-01-17', seats: 180 }
            ];
        }
        
        if (passengers.length === 0) {
            passengers = [
                { id: 1, name: 'John Doe', nationality: 'India', gender: 'Male', passport: 'A12345678', phone: '9876543210', address: 'Delhi, India' },
                { id: 2, name: 'Jane Smith', nationality: 'USA', gender: 'Female', passport: 'B87654321', phone: '1234567890', address: 'Mumbai, India' },
                { id: 3, name: 'Mike Johnson', nationality: 'UK', gender: 'Male', passport: 'C11223344', phone: '5555555555', address: 'Bangalore, India' }
            ];
        }
        
        if (bookings.length === 0) {
            bookings = [
                { id: 1, passengerId: 1, passengerName: 'John Doe', flightCode: 'FA001', gender: 'Male', passport: 'A12345678', nationality: 'India', amount: 5000 },
                { id: 2, passengerId: 2, passengerName: 'Jane Smith', flightCode: 'FA002', gender: 'Female', passport: 'B87654321', nationality: 'USA', amount: 4500 }
            ];
        }
        
        saveData();
        displayFlights();
        displayPassengers();
        displayBookings();
        updateDropdowns();
        loadDashboardStats();
    } catch (error) {
        console.error('Add sample data error:', error);
    }
}

// Initialize with sample data if no data exists
if (localStorage.getItem('flights') === null) {
    addSampleData();
}

// Delay Prediction Functionality
const API_URL = 'http://localhost:5000/api';

// Sample flight data for predictions
const sampleFlights = [
    {
        airline: 'AA',
        origin_airport: 'JFK',
        dest_airport: 'LAX',
        departure_time: new Date(Date.now() + 24*60*60*1000).toISOString().slice(0, 16),
        distance: 2500,
        temperature: 75,
        wind_speed: 10,
        visibility: 10,
        precipitation: 0,
        origin_congestion: 0.7,
        dest_congestion: 0.6
    },
    {
        airline: 'DL',
        origin_airport: 'ATL',
        dest_airport: 'ORD',
        departure_time: new Date(Date.now() + 24*60*60*1000).toISOString().slice(0, 16),
        distance: 600,
        temperature: 80,
        wind_speed: 15,
        visibility: 8,
        precipitation: 0.1,
        origin_congestion: 0.8,
        dest_congestion: 0.5
    },
    {
        airline: 'UA',
        origin_airport: 'SFO',
        dest_airport: 'DEN',
        departure_time: new Date(Date.now() + 24*60*60*1000).toISOString().slice(0, 16),
        distance: 950,
        temperature: 65,
        wind_speed: 20,
        visibility: 12,
        precipitation: 0,
        origin_congestion: 0.6,
        dest_congestion: 0.4
    }
];

// Check API status on page load
function checkApiStatus() {
    const statusElement = document.getElementById('api-status');
    if (!statusElement) return;
    
    fetch(`${API_URL}/health`)
        .then(response => response.json())
        .then(data => {
            if (data.status === 'healthy') {
                statusElement.textContent = 'API Online';
                statusElement.className = 'status-indicator status-online';
            } else {
                throw new Error('API not healthy');
            }
        })
        .catch(error => {
            console.error('API Status Check Error:', error);
            statusElement.textContent = 'API Offline';
            statusElement.className = 'status-indicator status-offline';
        });
}

// Handle delay prediction form submission
function handleDelayPredictionSubmit(e) {
    e.preventDefault();
    
    const formData = new FormData(e.target);
    const flightData = {
        airline: formData.get('airline'),
        origin_airport: formData.get('origin_airport'),
        dest_airport: formData.get('dest_airport'),
        departure_time: new Date(formData.get('departure_time')).toISOString(),
        temperature: parseFloat(formData.get('temperature')) || 75,
        wind_speed: parseFloat(formData.get('wind_speed')) || 10,
        visibility: parseFloat(formData.get('visibility')) || 10,
        precipitation: parseFloat(formData.get('precipitation')) || 0,
        origin_congestion: 0.7, // Default values
        dest_congestion: 0.6
    };
    
    // Calculate distance based on airports (simplified)
    flightData.distance = calculateDistance(flightData.origin_airport, flightData.dest_airport);
    
    predictDelay(flightData);
}

// Predict delay for flight data
function predictDelay(flightData) {
    const statusElement = document.getElementById('api-status');
    
    // Check if API is online
    if (statusElement && statusElement.classList.contains('status-offline')) {
        // Use simulation if API is offline
        const prediction = simulatePrediction(flightData);
        displayPrediction(prediction, 'predictionResult');
        return;
    }
    
    fetch(`${API_URL}/predict-delay`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(flightData)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
    })
    .then(prediction => {
        displayPrediction(prediction, 'predictionResult');
    })
    .catch(error => {
        console.error('Prediction Error:', error);
        // Fallback to simulation
        const prediction = simulatePrediction(flightData);
        displayPrediction(prediction, 'predictionResult');
        showMessage('Using simulated prediction (API unavailable)', 'error');
    });
}

// Simulate prediction for demo purposes
function simulatePrediction(flightData) {
    const baseRisk = Math.random() * 0.6 + 0.2; // 20-80% base risk
    
    // Adjust risk based on weather conditions
    let riskAdjustment = 0;
    if (flightData.wind_speed > 25) riskAdjustment += 0.1;
    if (flightData.precipitation > 0.5) riskAdjustment += 0.15;
    if (flightData.visibility < 5) riskAdjustment += 0.1;
    if (flightData.temperature < 32 || flightData.temperature > 95) riskAdjustment += 0.05;
    
    const finalRisk = Math.min(0.95, baseRisk + riskAdjustment);
    const riskScore = Math.round(finalRisk * 100);
    
    let riskLevel = 'Low';
    if (riskScore > 66) riskLevel = 'High';
    else if (riskScore > 33) riskLevel = 'Medium';
    
    const departureDate = new Date(flightData.departure_time);
    const dayOfWeek = departureDate.toLocaleDateString('en-US', { weekday: 'long' });
    const month = departureDate.toLocaleDateString('en-US', { month: 'long' });
    
    return {
        delay_probability: finalRisk,
        risk_level: riskLevel,
        risk_score: riskScore,
        flight_info: {
            airline: flightData.airline,
            route: `${flightData.origin_airport} → ${flightData.dest_airport}`,
            departure_time: flightData.departure_time,
            day_of_week: dayOfWeek,
            month: month
        },
        recommendations: getRecommendations(riskLevel, flightData)
    };
}

// Get recommendations based on risk level
function getRecommendations(riskLevel, flightData) {
    const recommendations = [];
    
    if (riskLevel === 'High') {
        recommendations.push('Consider rebooking to an earlier flight');
        recommendations.push('Allow extra time for connections');
        recommendations.push('Check weather conditions regularly');
        recommendations.push('Consider travel insurance');
    } else if (riskLevel === 'Medium') {
        recommendations.push('Monitor flight status closely');
        recommendations.push('Allow some buffer time for connections');
        recommendations.push('Check weather forecast');
    } else {
        recommendations.push('Flight likely to depart on time');
        recommendations.push('Standard arrival planning recommended');
    }
    
    if (flightData.precipitation > 0.3) {
        recommendations.push('Weather-related delays possible');
    }
    
    if (flightData.wind_speed > 20) {
        recommendations.push('High winds may cause delays');
    }
    
    return recommendations;
}

// Display prediction results
function displayPrediction(prediction, elementId) {
    const resultElement = document.getElementById(elementId);
    if (!resultElement) return;
    
    const riskLevelElement = document.getElementById('riskLevel');
    const riskScoreElement = document.getElementById('riskScore');
    const flightRouteElement = document.getElementById('flightRoute');
    const flightInfoElement = document.getElementById('flightInfo');
    const recommendationsListElement = document.getElementById('recommendationsList');
    
    if (riskLevelElement) {
        riskLevelElement.textContent = `${prediction.risk_level} Risk`;
        riskLevelElement.className = `risk-level risk-${prediction.risk_level.toLowerCase()}`;
    }
    
    if (riskScoreElement) {
        riskScoreElement.textContent = `${prediction.risk_score}%`;
    }
    
    if (flightRouteElement) {
        flightRouteElement.textContent = prediction.flight_info.route;
    }
    
    if (flightInfoElement) {
        const departureDate = new Date(prediction.flight_info.departure_time);
        flightInfoElement.innerHTML = `
            <div><strong>Airline:</strong> ${prediction.flight_info.airline}</div>
            <div><strong>Departure:</strong> ${departureDate.toLocaleString()}</div>
            <div><strong>Day:</strong> ${prediction.flight_info.day_of_week}</div>
        `;
    }
    
    if (recommendationsListElement) {
        recommendationsListElement.innerHTML = prediction.recommendations
            .map(rec => `<li>${rec}</li>`)
            .join('');
    }
    
    resultElement.style.display = 'block';
    resultElement.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// Predict sample flight
function predictSampleFlight(index) {
    if (index >= 0 && index < sampleFlights.length) {
        const flightData = { ...sampleFlights[index] };
        
        // Use current API to predict or simulate
        const statusElement = document.getElementById('api-status');
        if (statusElement && statusElement.classList.contains('status-online')) {
            predictDelay(flightData);
        } else {
            const prediction = simulatePrediction(flightData);
            displaySamplePrediction(prediction, index);
        }
    }
}

// Display sample prediction results
function displaySamplePrediction(prediction, index) {
    const resultsContainer = document.getElementById('samplePredictionResults');
    if (!resultsContainer) return;
    
    const resultCard = document.createElement('div');
    resultCard.className = 'sample-result-card';
    resultCard.innerHTML = `
        <div class="sample-result-header">
            <strong>${prediction.flight_info.route}</strong>
            <span class="sample-risk-badge risk-${prediction.risk_level.toLowerCase()}">
                ${prediction.risk_level} Risk (${prediction.risk_score}%)
            </span>
        </div>
        <div class="sample-recommendations">
            <strong>Recommendations:</strong>
            <ul>
                ${prediction.recommendations.map(rec => `<li>${rec}</li>`).join('')}
            </ul>
        </div>
    `;
    
    resultsContainer.appendChild(resultCard);
    resultCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

// Calculate approximate distance between airports (simplified)
function calculateDistance(origin, dest) {
    const distances = {
        'JFK-LAX': 2500, 'LAX-JFK': 2500,
        'ATL-ORD': 600, 'ORD-ATL': 600,
        'SFO-DEN': 950, 'DEN-SFO': 950,
        'JFK-SFO': 2600, 'SFO-JFK': 2600,
        'LAX-ORD': 1750, 'ORD-LAX': 1750,
        'ATL-LAX': 1950, 'LAX-ATL': 1950
    };
    
    const key = `${origin}-${dest}`;
    return distances[key] || 1000; // Default distance
}

// Clear delay prediction form
function clearDelayPredictionForm() {
    const form = document.getElementById('delayPredictionForm');
    if (form) {
        form.reset();
        
        // Set default values
        document.getElementById('temperature').value = '75';
        document.getElementById('windSpeed').value = '10';
        document.getElementById('visibility').value = '10';
        document.getElementById('precipitation').value = '0';
        
        // Set default departure time to tomorrow
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        tomorrow.setHours(8, 0, 0, 0);
        document.getElementById('departureTime').value = tomorrow.toISOString().slice(0, 16);
    }
    
    // Hide prediction result
    const resultElement = document.getElementById('predictionResult');
    if (resultElement) {
        resultElement.style.display = 'none';
    }
    
    // Clear sample results
    const sampleResults = document.getElementById('samplePredictionResults');
    if (sampleResults) {
        sampleResults.innerHTML = '';
    }
}

// Initialize delay prediction functionality
function initializeDelayPrediction() {
    // Check API status
    checkApiStatus();
    
    // Setup form handler
    const delayForm = document.getElementById('delayPredictionForm');
    if (delayForm) {
        delayForm.addEventListener('submit', handleDelayPredictionSubmit);
    }
    
    // Set default departure time
    const departureTimeInput = document.getElementById('departureTime');
    if (departureTimeInput) {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        tomorrow.setHours(8, 0, 0, 0);
        departureTimeInput.value = tomorrow.toISOString().slice(0, 16);
    }
    
    // Check API status periodically
    setInterval(checkApiStatus, 30000); // Check every 30 seconds
}

// Update the main initialization to include delay prediction
const originalDOMContentLoaded = document.addEventListener;
document.addEventListener('DOMContentLoaded', function() {
    try {
        initializeNavigation();
        loadDashboardStats();
        loadAllData();
        setupFormHandlers();
        setupModalHandlers();
        initializeDelayPrediction(); // Add delay prediction initialization
    } catch (error) {
        console.error('Initialization error:', error);
        showMessage('Application initialization failed. Please refresh the page.', 'error');
    }
});