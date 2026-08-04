# Falcon Airlines Enterprise — User Analysis & Agile User Stories

## 1. User Types

### 1.1 Admin

| Attribute | Description |
|-----------|-------------|
| **Responsibilities** | Configure and govern the platform; manage users, roles, airports, aircraft, schedules, fare products, inventory, payments, integrations, compliance, and global platform settings. |
| **Permissions** | Full read/write access across all modules; role and permission management; system configuration; audit log review; pricing and inventory overrides; operational reporting. |
| **Daily Workflow** | Review overnight sales and operational dashboards; approve fare and schedule changes; monitor system health; handle escalations; manage user accounts and access; run compliance and fraud checks. |
| **Pain Points** | Time-consuming manual configuration; difficulty tracing changes across teams; risk of misconfigured inventory or fares; alert fatigue; slow audit and reconciliation. |
| **Goals** | Maintain a stable, secure, compliant booking platform; reduce time-to-market for commercial changes; minimize revenue leakage and operational errors; enable self-service for agents. |

### 1.2 Airline Agent

| Attribute | Description |
|-----------|-------------|
| **Responsibilities** | Assist passengers via call center, airport counter, or chat; search and book flights; modify or cancel reservations; process payments, refunds, and exchanges; check in passengers; resolve disruptions. |
| **Permissions** | Booking, payment, refund, and ticketing operations within assigned scope; PNR read/write for own station or queue; limited schedule/inventory view; no global admin or config rights. |
| **Daily Workflow** | Log in to queue-based workbench; handle incoming calls/chats; search flights; create and modify PNRs; collect payment; issue tickets; process changes/cancellations; handle rebooking after disruptions. |
| **Pain Points** | Slow search and booking flow; inconsistent data between channels; complicated refund rules; difficulty accessing passenger history; duplicate profile entries; manual rebooking during IROPs. |
| **Goals** | Serve passengers quickly and accurately; maximize first-call resolution; reduce average handle time; increase sales and ancillary attachment; protect revenue through correct policies. |

### 1.3 Customer

| Attribute | Description |
|-----------|-------------|
| **Responsibilities** | Search, compare, book, pay, manage, and travel; provide accurate passenger and contact information; comply with travel document and check-in deadlines. |
| **Permissions** | View public schedules, fares, and personal bookings; create and manage own PNRs; make payments and request changes/cancellations; check in and download boarding pass. |
| **Daily Workflow** | Search for flights; select dates, cabin, and extras; enter passenger details; pay; receive confirmation; modify if needed; check in online; travel. |
| **Pain Points** | Price discrepancies across devices; confusing fees; difficulty finding flexible options; slow or failed checkout; lack of real-time disruption updates; poor refund visibility. |
| **Goals** | Find the best fare and schedule; complete booking quickly; manage trips easily; receive timely notifications; travel with minimal friction; earn and use loyalty benefits. |

---

## 2. Agile User Stories

### Authentication

1. As an admin, I want to create user accounts and assign roles so that the right people have the right access.
2. As an admin, I want to enforce MFA for agents and admins so that sensitive operations are protected.
3. As an airline agent, I want to log in with my employee credentials so that I can access my assigned work queues.
4. As a customer, I want to register an account with my email or phone so that I can manage my bookings and loyalty profile.
5. As a customer, I want to reset my password securely so that I can regain access if I forget it.
6. As an admin, I want to review login and action audit logs so that I can detect and investigate suspicious activity.
7. As a customer, I want to log in with social or SSO credentials so that I can access my account without creating a new password.
8. As an admin, I want to disable accounts automatically after failed attempts so that brute force attacks are prevented.
9. As an airline agent, I want my session to timeout after inactivity so that unattended terminals are not misused.
10. As an admin, I want to configure password and session policies so that security standards are enforced globally.

### Flight Management

11. As an admin, I want to add and edit flight schedules so that customers can book new routes.
12. As an admin, I want to assign aircraft and seat maps to flights so that inventory is accurate.
13. As an admin, I want to configure fare classes and inventory levels so that revenue targets are met.
14. As an airline agent, I want to see real-time seat availability so that I can sell accurate inventory.
15. As a customer, I want to filter flights by stops, time, and price so that I can find suitable options.
16. As a customer, I want to compare fare families so that I understand what each option includes.
17. As an admin, I want to open and close booking classes so that demand can be managed dynamically.
18. As an admin, I want to schedule code-share and interline flights so that partner inventory is shown.
19. As an airline agent, I want to put flights on sale or remove them from sale so that schedules can be controlled.
20. As a customer, I want to see flight status and on-time performance so that I can make informed choices.

### Airport Management

21. As an admin, I want to maintain airport master data so that routes, terminals, and gates are accurate.
22. As an admin, I want to configure check-in and boarding cutoff times per airport so that policies are enforced.
23. As an airline agent, I want to see check-in counter and gate assignments so that I can assist passengers.
24. As an admin, I want to manage baggage rules by airport and route so that fees and allowances are correct.
25. As an airline agent, I want to record and track special service requests at airports so that vulnerable passengers are assisted.
26. As an admin, I want to link airports to operating regions and tax authorities so that taxes are applied correctly.
27. As an admin, I want to define ground handling and service providers per airport so that operations are coordinated.
28. As a customer, I want to see terminal and gate information on my boarding pass so that I can navigate the airport.
29. As an admin, I want to set airport-specific operational hours so that bookings respect local constraints.
30. As an airline agent, I want to view baggage drop and security wait information so that I can advise passengers.

### Passenger Management

31. As a customer, I want to create a passenger profile so that my details are saved for future bookings.
32. As a customer, I want to add passport, visa, and contact details to my profile so that check-in is faster.
33. As an airline agent, I want to view and edit passenger information so that bookings can be corrected.
34. As an admin, I want to merge duplicate passenger profiles so that loyalty and contact data is clean.
35. As a customer, I want to add passengers to a booking on my behalf so that I can book family trips easily.
36. As an airline agent, I want to record special service requests so that passengers with needs are handled properly.
37. As a customer, I want to view and update my contact preferences so that I receive communications where I want them.
38. As an admin, I want to enforce data retention and privacy rules for passenger data so that we comply with GDPR.
39. As an airline agent, I want to search passenger history by name or booking reference so that I can resolve queries quickly.
40. As a customer, I want to link my loyalty membership to my profile so that I earn and redeem miles.

### Booking

41. As a customer, I want to search for one-way, round-trip, and multi-city flights so that I can plan any trip.
42. As a customer, I want to select seats and add extras during booking so that I get the services I need.
43. As a customer, I want to hold a reservation before paying so that I can confirm travel details with others.
44. As an airline agent, I want to create a booking on behalf of a customer so that I can serve non-digital passengers.
45. As an airline agent, I want to modify or cancel a booking so that I can process change requests.
46. As a customer, I want to view all my upcoming and past bookings so that I can manage my travel.
47. As a customer, I want to split or merge PNRs so that group and individual travel can be adjusted.
48. As an airline agent, I want to apply corporate and negotiated fares during booking so that contracted customers get the right price.
49. As a customer, I want to receive a clear booking confirmation with itinerary and fare rules so that I know what I purchased.
50. As an airline agent, I want to add group bookings and allotments so that corporate or tour groups are handled.

### Payment

51. As a customer, I want to pay with credit card, debit card, UPI, or wallet so that I have flexible options.
52. As a customer, I want my card data to be tokenized so that my payment information is secure.
53. As an airline agent, I want to collect payment from a customer and apply it to a booking so that the reservation is confirmed.
54. As an admin, I want to reconcile payments and refunds daily so that revenue accounting is accurate.
55. As a customer, I want to receive a refund or credit when I cancel so that I understand the outcome.
56. As an airline agent, I want to process partial refunds and chargebacks so that exceptions are handled correctly.
57. As a customer, I want to use loyalty points or vouchers for payment so that I can reduce cash outlay.
58. As an admin, I want to configure payment gateways and routing by market so that transactions succeed locally.
59. As an airline agent, I want to see payment status on a booking so that I can take the next step.
60. As a customer, I want an invoice and tax receipt for my booking so that I can claim expenses.

### Ticketing

61. As a customer, I want my ticket to be issued automatically after payment so that my reservation is confirmed.
62. As an airline agent, I want to issue, void, or reissue tickets so that I can correct errors and changes.
63. As an admin, I want to configure ticketing time limits so that bookings are not held indefinitely without payment.
64. As a customer, I want to receive my e-ticket by email and in my account so that I can travel without printing.
65. As an airline agent, I want to revalidate tickets after schedule changes so that bookings remain valid.
66. As a customer, I want to add ancillary services like baggage, meals, and Wi-Fi to my ticket so that I can customize my trip.
67. As an admin, I want to generate EMDs for ancillary services so that revenue accounting is clear.
68. As an airline agent, I want to refund or exchange tickets according to fare rules so that policies are applied consistently.
69. As a customer, I want to see my ticket status and validity so that I know my booking is active.
70. As an admin, I want to integrate with IATA BSP or ARC for ticket reporting so that settlement is correct.

### Delay Prediction

71. As an admin, I want to see predicted delay risk for flights so that proactive decisions can be made.
72. As an airline agent, I want to be notified of predicted delays so that I can inform passengers early.
73. As a customer, I want to receive delay predictions and updates so that I can plan accordingly.
74. As an admin, I want to view delay trend analytics so that recurring operational issues can be addressed.
75. As an airline agent, I want the system to suggest rebooking options for delayed passengers so that disruption handling is faster.

### Analytics

76. As an admin, I want a sales dashboard by channel, route, and cabin so that I can track performance.
77. As an admin, I want to see load factors and revenue per flight so that capacity decisions are informed.
78. As an admin, I want to report on ancillary attachment and revenue so that product bundles can be optimized.
79. As an admin, I want to monitor search-to-book conversion so that funnel issues are identified.
80. As an admin, I want to export reports for finance and revenue accounting so that month-end closes are smooth.
81. As an admin, I want to track agent productivity and handle times so that staffing and training are improved.
82. As an admin, I want to see refund and chargeback trends so that revenue leakage is controlled.
83. As an admin, I want to analyze no-show and cancellation patterns so that overbooking can be tuned.
84. As an admin, I want real-time alerts on booking anomalies so that fraud and errors are caught early.
85. As an admin, I want to compare year-over-year and route-level metrics so that strategic plans are supported.

### Notifications

86. As a customer, I want booking confirmations by email and SMS so that I have proof of purchase.
87. As a customer, I want check-in reminders so that I do not miss deadlines.
88. As a customer, I want real-time flight status and gate change alerts so that I stay informed.
89. As an airline agent, I want disruption notifications for my assigned passengers so that I can act quickly.
90. As a customer, I want to choose my notification channel and language so that communication is convenient.
91. As an admin, I want to configure notification templates and triggers so that messaging is consistent.
92. As a customer, I want boarding pass reminders so that I am ready at the gate.
93. As a customer, I want refund and change status updates so that I know the progress of my request.
94. As an airline agent, I want to send targeted notifications to passengers on a flight so that operational communications are coordinated.
95. As an admin, I want delivery and open tracking for notifications so that I can measure reach.

### Admin Dashboard

96. As an admin, I want a centralized dashboard with KPIs so that I can monitor the business at a glance.
97. As an admin, I want to manage users, roles, and permissions from the dashboard so that access control is simple.
98. As an admin, I want to view and approve fare and schedule changes from the dashboard so that commercial moves are controlled.
99. As an admin, I want to see system health, errors, and integration status so that incidents are visible.
100. As an admin, I want to access audit logs and compliance reports from the dashboard so that governance is easy.
101. As an admin, I want to configure global business rules and thresholds from the dashboard so that the platform adapts without code changes.
102. As an admin, I want to manage partner and API keys from the dashboard so that integrations are secure and traceable.
