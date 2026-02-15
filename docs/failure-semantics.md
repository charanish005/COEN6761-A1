# Failure Semantics in Concurrent Microservice Fan-out/Fan-in

This project implements and tests three explicit exception-handling policies for concurrent execution using Java `CompletableFuture`. In concurrent systems, execution and completion order are nondeterministic, so the system must define what happens when one or more microservices fail.

## 1) Fail-Fast (Atomic Policy)
**Definition:** If any concurrent microservice invocation fails, the entire operation fails and no result is produced.

**When it is appropriate:**
- correctness-critical systems (payments, authentication, transactional workflows)
- cases where partial results are invalid or unsafe

**Risk / trade-off:**
- reduced availability: one failing microservice causes the whole request to fail

## 2) Fail-Partial (Best-Effort Policy)
**Definition:** Each microservice failure is handled independently. Successful results are returned while failed results are omitted. The overall computation completes normally and no exception escapes to the caller.

**When it is appropriate:**
- dashboards, analytics, aggregation views
- cases where partial data is still useful

**Risk / trade-off:**
- missing data can lead to misleading conclusions if the caller assumes the output is complete

## 3) Fail-Soft (Fallback Policy)
**Definition:** Failures are replaced with a predefined fallback value. The overall computation always completes normally.

**When it is appropriate:**
- high-availability systems where degraded output is acceptable
- user-facing systems that prefer “something” over an error (e.g., cached/default values)

**Risk / trade-off (important):**
- can mask serious failures and outages if fallback values look normal
- should be paired with monitoring/logging in real systems to detect hidden failures.
This project demonstrates how explicit failure semantics improve reliability in concurrent systems.

