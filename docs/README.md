# Falcon Airlines - Airline Management System

A modern, responsive web application for managing airline operations including flights, passengers, bookings, and cancellations.

## Features

### 🛩️ Flight Management
- Add, edit, and delete flights
- Track flight details (code, source, destination, date, seats)
- View all flights in a comprehensive table

### 👥 Passenger Management
- Register new passengers with complete details
- Edit and delete passenger information
- Track passenger demographics and contact information

### 🎫 Ticket Booking
- Book tickets for passengers on available flights
- Automatic passenger information population
- Track booking amounts and details

### ❌ Cancellation Management
- Cancel booked tickets
- Maintain cancellation records
- Automatic booking removal upon cancellation

### 📊 Dashboard
- Real-time statistics overview
- Quick action buttons for common tasks
- Visual representation of system data

## Technology Stack

- **Frontend**: HTML5, CSS3, JavaScript (ES6+)
- **Styling**: Custom CSS with responsive design
- **Icons**: Font Awesome
- **Fonts**: Google Fonts (Poppins)
- **Storage**: Local Storage for data persistence
- **Deployment**: Static hosting ready

## File Structure

```
airline-management-website/
├── index.html          # Main HTML file
├── styles.css          # CSS styling
├── script.js           # JavaScript functionality
└── README.md           # Project documentation
```

## Getting Started

### Local Development

1. **Clone or download the project files**
   ```bash
   # If using git
   git clone <repository-url>
   cd airline-management-website
   ```

2. **Open the application**
   - Simply open `index.html` in your web browser
   - Or use a local server for better development experience

3. **Using a local server (recommended)**
   ```bash
   # Using Python
   python -m http.server 8000
   
   # Using Node.js (if you have http-server installed)
   npx http-server
   
   # Using PHP
   php -S localhost:8000
   ```

4. **Access the application**
   - Open your browser and go to `http://localhost:8000`

## Deployment Options

### 1. GitHub Pages (Free)

1. **Create a GitHub repository**
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/yourusername/airline-management.git
   git push -u origin main
   ```

2. **Enable GitHub Pages**
   - Go to your repository settings
   - Scroll down to "GitHub Pages" section
   - Select "main" branch as source
   - Your site will be available at `https://yourusername.github.io/repository-name`

### 2. Netlify (Free)

1. **Drag and Drop Method**
   - Go to [netlify.com](https://netlify.com)
   - Sign up/Login
   - Drag your project folder to the deploy area
   - Your site will be live instantly

2. **Git Integration**
   - Connect your GitHub repository
   - Netlify will automatically deploy on every push

### 3. Vercel (Free)

1. **Install Vercel CLI**
   ```bash
   npm install -g vercel
   ```

2. **Deploy**
   ```bash
   vercel
   ```

### 4. Firebase Hosting

1. **Install Firebase CLI**
   ```bash
   npm install -g firebase-tools
   ```

2. **Initialize and deploy**
   ```bash
   firebase login
   firebase init hosting
   firebase deploy
   ```

### 5. Traditional Web Hosting

1. **Upload files to your web server**
   - Upload `index.html`, `styles.css`, and `script.js` to your web hosting
   - Ensure all files are in the same directory

2. **Access your website**
   - Your site will be available at your domain

## Features in Detail

### Dashboard
- **Statistics Cards**: Display total flights, passengers, bookings, and cancellations
- **Quick Actions**: Direct access to add flights, passengers, book tickets, and cancel tickets
- **Responsive Design**: Works perfectly on desktop, tablet, and mobile devices

### Flight Management
- **Add Flights**: Complete form with validation
- **Edit Flights**: Click edit button to modify existing flights
- **Delete Flights**: Remove flights with confirmation
- **Flight Table**: Comprehensive view of all flights

### Passenger Management
- **Add Passengers**: Register new passengers with all required details
- **Edit Passengers**: Modify passenger information
- **Delete Passengers**: Remove passengers with confirmation
- **Passenger Table**: View all registered passengers

### Ticket Booking
- **Select Passenger**: Dropdown with all registered passengers
- **Auto-populate**: Passenger details automatically fill when selected
- **Select Flight**: Choose from available flights
- **Set Amount**: Enter ticket price
- **Booking Table**: View all bookings

### Cancellation System
- **Select Ticket**: Choose from active bookings
- **Auto-populate**: Flight code automatically fills
- **Set Date**: Choose cancellation date
- **Cancellation Table**: Track all cancellations

## Data Persistence

The application uses **localStorage** to persist data:
- Data survives browser restarts
- No server required
- Works offline
- Data is stored locally on the user's device

## Browser Compatibility

- ✅ Chrome (recommended)
- ✅ Firefox
- ✅ Safari
- ✅ Edge
- ✅ Mobile browsers

## Customization

### Colors
The application uses a blue and orange color scheme. To customize:

1. **Primary Blue**: `#1d4887`
2. **Secondary Orange**: `#fcb130`
3. **Gradient Background**: `linear-gradient(135deg, #667eea 0%, #764ba2 100%)`

### Adding New Features
The modular JavaScript structure makes it easy to add new features:

1. **Add new data arrays** in `script.js`
2. **Create new display functions** following the existing pattern
3. **Add form handlers** for new functionality
4. **Update the navigation** in `index.html`

## Troubleshooting

### Common Issues

1. **Page not loading**
   - Ensure all files are in the same directory
   - Check browser console for errors
   - Try using a local server instead of file:// protocol

2. **Data not saving**
   - Check if localStorage is enabled in your browser
   - Clear browser cache and try again

3. **Styling issues**
   - Ensure `styles.css` is properly linked
   - Check if Font Awesome and Google Fonts are loading

4. **Mobile responsiveness**
   - Test on different screen sizes
   - Check CSS media queries

## Contributing

Feel free to contribute to this project by:
- Reporting bugs
- Suggesting new features
- Improving the code
- Enhancing the design

## License

This project is open source and available under the MIT License.

## Support

For support or questions:
- Create an issue in the repository
- Contact the development team
- Check the documentation

---

**Falcon Airlines Management System** - Making airline management simple and efficient! ✈️