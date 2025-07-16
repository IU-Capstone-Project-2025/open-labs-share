# Marimo Python Service

## Purpose

This service is a dedicated gRPC server responsible for executing code within a Marimo kernel. It is designed to be a stateless execution engine, managed entirely by the `marimo-manager-service`.

It receives requests to start sessions, execute cells, and retrieve state, performing these actions in an isolated Python environment.

## Database Entities

This service is stateless and does not connect to a database directly. All state and persistence are handled by the `marimo-manager-service`. 