Lilach Project 🌸

Lilach is a client–server flower store management system built as a full-stack Java application. It provides both customer-facing features (catalog, basket, checkout, VIP subscriptions) and management tools (user accounts, orders, reports).

The system uses a JavaFX client with FXML-based views and controllers, and a Java server built on OCSF. Client–server communication is handled through a unified Msg system, with EventBus managing events in the client. On the backend, Hibernate ORM is used with a MySQL database to handle persistence of users, products, orders, and payments.

Features

User Accounts

Registration, login, and role-based access

Account activation/freeze/unfreeze

VIP subscription with recurring monthly charge

Catalog and Products

Dynamic product catalog with category filtering and discounts

Custom bouquet builder with selectable pots/vases linked to catalog pricing

Basket and Orders

Add/remove items, update quantities directly in the basket

Orders with delivery or branch pickup options

Track order status (Awaiting Completion / Completed)

Payments

Shared PaymentController with validation of card details

Used for both purchases and VIP subscriptions

Reports and Management

Filter and view orders (Mine, All Users, All Orders)

View system-wide spending and user activity

Access to product-level order details

UI

JavaFX full-screen views with consistent top-bar layout

Adaptive navigation per screen

Technology Stack

Frontend: JavaFX (FXML, EventBus)

Backend: Java with OCSF

Database: MySQL with Hibernate ORM

Build Tool: Maven

Languages: Java, SQL

Project Scope

The Lilach Project demonstrates how to build a complete information system with:

A modular client–server architecture

Database persistence through Hibernate

A responsive JavaFX interface

Integrated business logic for both customers and managers
