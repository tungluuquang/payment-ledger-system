# Payment Ledger System

A mini payment ledger system built to study financial-domain design patterns, distributed transactions, and event-driven architecture.

## Overview

This project simulates a payment platform with multiple services such as account management, ledger recording, saga orchestration, fraud checking, projections, authentication, API gateway, and frontend.

The main goal is to explore how financial systems handle consistency, transaction flow, and reliable ledger updates in a microservices architecture.

## Main Features

* Account service
* Ledger service
* Saga service for distributed transaction orchestration
* Fraud check service
* Projection service
* API Gateway
* Authorization server
* Frontend application
* Docker-based local development

## Tech Stack

* Java / Spring Boot
* React
* PostgreSQL
* Docker & Docker Compose

## Getting Started

### Prerequisites

* Docker
* Docker Compose

### Build and Run

Clone the repository and start all services:

```bash
docker compose up --build
```

After the containers are started, the application and supporting services will be available according to the ports defined in `docker-compose.yml`.

## Future Plan

* Implement payment-ledger reconciliation for consistency verification.
* Add real-time fraud detection using big data technologies and machine learning.
* Deploy services on Kubernetes.
* Deploy and scale the platform on Google Cloud Platform (GKE).
* Improve project documentation, architecture diagrams, and API specifications.

## Purpose

This project is mainly for learning and experimentation with financial system design, ledger consistency, saga patterns, event-driven architecture, and future big data extensions.
