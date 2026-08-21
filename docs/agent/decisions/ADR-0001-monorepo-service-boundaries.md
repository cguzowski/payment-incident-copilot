# ADR-0001: Monorepo with independently deployable applications

Status: Accepted  
Date: 2026-08-20  
Decision owner: Christopher Guzowski

## Context

The MVP contains an Angular operator console, a Spring Boot copilot API, and a
Spring Boot synthetic MCP server. Development should remain convenient for one
developer without erasing the runtime boundaries demonstrated by the project.

## Decision drivers

- One clone and one reviewable portfolio repository
- Simple coordinated changes across UI and service contracts
- Independent build and deployment boundaries
- Minimal repository administration for a solo project

## Considered options

### Separate repositories

- Advantages: Strong isolation and independent release history
- Disadvantages: More coordination and setup than the MVP warrants

### One repository and one combined application

- Advantages: Minimal initial project count
- Disadvantages: Blurs UI, workflow, and synthetic source-system boundaries

### Monorepo with separate applications

- Advantages: Convenient development with explicit runtime boundaries
- Disadvantages: CI and root configuration must understand multiple toolchains

## Decision

Use one Git repository containing three independently buildable and deployable
applications. Use a Maven aggregator for the two Java services. Keep Angular
under its own npm workspace.

## Consequences

- Cross-application changes can be reviewed atomically.
- Service-specific instructions and tests remain local to each application.
- Deployment must not assume that sharing a repository means sharing a
  container or process.

## Revisit trigger

Revisit only if independent teams, security boundaries, or release cadences
make repository-level coupling measurably harmful.
