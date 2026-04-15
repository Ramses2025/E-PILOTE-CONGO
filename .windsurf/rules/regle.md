---
trigger: always_on
---

Objective

Ensure every implementation, bug fix, or modification strictly aligns with official, up-to-date documentation to guarantee correctness, maintainability, and production readiness.

Rule

For every task (feature development, bug fixing, refactoring, configuration, or integration), the system must:

1. Mandatory Documentation Check

Before writing or modifying any code, always consult the official documentation of:

Kotlin Multiplatform
Spring Boot (Kotlin usage)
Couchbase (including Capella and App Services)
2. Strict Compliance
Follow official APIs, patterns, and recommended architectures only
Do not invent implementations when a documented approach exists
Prefer documented best practices over assumptions
3. Verification Before Implementation

Before coding:

Confirm the approach is valid according to documentation
Ensure compatibility with:
Kotlin Multiplatform constraints
Spring Boot (Kotlin) conventions
Couchbase / Capella architecture
4. During Implementation
Apply idiomatic Kotlin patterns
Respect Spring Boot standards (configuration, injection, controllers)
Follow Couchbase best practices (sync, auth, data modeling)
5. After Implementation
Re-check documentation to validate:
correctness
security
recommended usage
Prohibited Behavior
Coding without referencing official documentation
Using outdated, deprecated, or unofficial patterns
Guessing APIs or configurations
Introducing architecture not aligned with documented practices
Expected Outcome
Code is aligned with official standards
Reduced bugs and inconsistencies
Better long-term maintainability
Production-ready quality by default
Enforcement

This rule is mandatory and systematic:
It applies to every action, without exception.