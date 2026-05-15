# TalkifyX

## Real-Time Chat Platform using Spring Boot Microservices

TalkifyX is a production-ready real-time chat application built using a microservices architecture with Spring Boot, WebSocket communication, Dockerized deployment, AWS cloud services, and CI/CD automation.

The platform supports secure authentication, real-time messaging, room management, Firebase notifications, media uploads, and scalable cloud-native deployment.

---

# Features

- Real-time chat using WebSocket + STOMP + SockJS
    
- Group chats and Direct Messages
    
- JWT-based authentication and authorization
    
- Google OAuth2 login integration
    
- Firebase push notifications
    
- User presence tracking (ONLINE / AWAY / DND / INVISIBLE)
    
- Media upload support using AWS S3
    
- Read receipts and typing indicators
    
- Room and member management
    
- Dockerized microservices deployment
    
- CI/CD automation using GitHub Actions
    
- HTTPS-enabled production deployment
    
- API Gateway routing with Spring Cloud Gateway
    
- Service discovery using Netflix Eureka
    

---

# Production Architecture

```text
Angular Frontend (AWS S3 + CloudFront HTTPS)
                    ↓
        https://talkifyx.duckdns.org
                    ↓
             Nginx Reverse Proxy
                    ↓
         Spring Cloud API Gateway
                    ↓
        Spring Boot Microservices
                    ↓
   MySQL + Redis + Firebase + AWS S3
```

---

# Microservices Architecture

|Service|Responsibility|
|---|---|
|API Gateway|Centralized routing, JWT validation, CORS|
|Eureka Server|Service discovery|
|Auth Service|Authentication, JWT, Google OAuth2|
|Room Service|Room and member management|
|Message Service|Message persistence and reactions|
|Chat Service|Real-time websocket communication|
|Presence Service|User online/offline tracking|
|Notification Service|Firebase push notifications|
|Media Service|AWS S3 media uploads|

---

# Technology Stack

|Category|Technologies|
|---|---|
|Frontend|Angular|
|Backend|Spring Boot|
|API Gateway|Spring Cloud Gateway|
|Service Discovery|Netflix Eureka|
|Real-Time Communication|WebSocket + STOMP + SockJS|
|Authentication|Spring Security + JWT + OAuth2|
|Database|MySQL|
|Cache|Redis|
|File Storage|AWS S3|
|Notifications|Firebase Cloud Messaging|
|Containerization|Docker + Docker Compose|
|Reverse Proxy|Nginx|
|CI/CD|GitHub Actions|
|Cloud Hosting|AWS EC2|
|Frontend Hosting|AWS S3 + CloudFront|
|HTTPS|Let’s Encrypt + Certbot|

---

# Real-Time Communication

TalkifyX uses:

- Spring WebSocket
    
- STOMP Protocol
    
- SockJS fallback
    

for real-time messaging features such as:

- instant chat updates
    
- typing indicators
    
- read receipts
    
- live room events
    
- presence updates
    

---

# Authentication & Security

## Authentication Features

- JWT token authentication
    
- Google OAuth2 login
    
- BCrypt password hashing
    
- Stateless session management
    

## Security Features

- HTTPS-enabled communication
    
- Secure WebSocket communication
    
- Gateway-level JWT validation
    
- CORS configuration
    
- Firebase secure integration
    

---

# Production Deployment

## Frontend Deployment

Frontend is deployed using:

- AWS S3 for static hosting
    
- AWS CloudFront as CDN
    
- HTTPS-enabled frontend delivery
    

---

## Backend Deployment

Backend microservices are deployed on:

- AWS EC2
    
- Docker containers
    
- Docker Compose 
    

---

## HTTPS & Reverse Proxy Setup

Production HTTPS was implemented using:

- DuckDNS
    
- Nginx
    
- Let’s Encrypt SSL
    
- Certbot
    

This solved:

- mixed content browser issues
    
- Firebase HTTPS restrictions
    
- secure websocket communication
    
- production security requirements
    

---

# CI/CD Pipeline

GitHub Actions is used for Continuous Integration and Deployment.

## CI/CD Flow

```text
Git Push
   ↓
GitHub Actions
   ↓
Build Application
   ↓
Build Docker Images
   ↓
Push Images to Docker Hub
   ↓
EC2 Pulls Latest Images
   ↓
Containers Restarted
```

---

# Docker Deployment

## Pull Latest Images

```bash
docker-compose pull
```

## Start Containers

```bash
docker-compose up -d
```

## Verify Running Containers

```bash
docker ps
```

---

# HTTPS Setup Commands

## Install Nginx

```bash
sudo yum install nginx -y
```

## Start Nginx

```bash
sudo systemctl start nginx
```

## Install Certbot

```bash
sudo yum install certbot python3-certbot-nginx -y
```

## Generate Free SSL Certificate

```bash
sudo certbot --nginx -d talkifyx.duckdns.org
```

---

## Live Deployment

### Frontend
https://d9idsjwmol8t5.cloudfront.net

### Backend API Gateway
https://talkifyx.duckdns.org

### Swagger API Documentation
https://talkifyx.duckdns.org/swagger-ui/index.html


---

# Deployment Challenges Solved

|Challenge|Solution|
|---|---|
|Frontend HTTPS + Backend HTTP conflict|Configured HTTPS backend using Nginx + SSL|
|Firebase not working on HTTP|Implemented HTTPS architecture|
|WebSocket production issues|Added websocket reverse proxy configuration|
|Angular route refresh issue|Configured SPA fallback routing|
|Frontend using localhost APIs|Fixed Angular production environment replacement|
|SSL certificate setup|Configured Let’s Encrypt + Certbot|

---

# Key Achievements

- Successfully deployed microservices architecture on AWS
    
- Implemented secure HTTPS communication
    
- Enabled real-time websocket communication
    
- Integrated Firebase notifications in production
    
- Automated deployment pipeline using GitHub Actions
    
- Containerized backend services using Docker
    
- Configured scalable reverse proxy architecture
    
- Enabled production-ready frontend hosting using CloudFront
    

---

# Future Improvements

- Kubernetes deployment
    
- Auto-scaling support
    
- Monitoring using Prometheus and Grafana
    
- Centralized logging
    
- Redis distributed caching
    
- Blue-Green deployment strategy
    
- Custom domain integration
    

---

# Project Structure

```text
TalkifyX/
├── api-gateway/
├── auth-service/
├── chat-service/
├── eureka-server/
├── media-service/
├── message-service/
├── notification-service/
├── presence-service/
├── room-service/
├── docker-compose.yml
├── init.sql
└── README.md
```

---

# Conclusion

TalkifyX demonstrates a complete production-grade microservices deployment architecture using Spring Boot, Docker, AWS Cloud Services, HTTPS security, WebSocket communication, and CI/CD automation.

The project showcases real-world cloud-native deployment practices including reverse proxy configuration, SSL security, container orchestration, real-time communication, and automated DevOps workflows.
