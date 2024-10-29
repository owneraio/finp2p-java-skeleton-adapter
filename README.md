© 2024 XCap Ecosystem Ltd trading as Ownera®. All rights reserved. SPDX-License-Identifier: Apache-2.0

# FinP2P Ledger Adapter Java

This is a sample project of a FinP2P ledger adapter on Java.
The goal is to show how to integrate with a FinP2P router, translating FinP2P instructions into ledger operations and providing a foundational framework for implementations.

The project is built around generated models and handlers derived from the FinP2P ledger operations as detailed in the "dlt-adapter-api.yaml" OpenAPI document.

In its current form, the project emulates ledger functionality by maintaining account states within memory. For genuine deployments, the sections within src/main/java/io.ownera.ledger.adapter.service.LedgerService need to be adapted for actual ledger interactions or integration with a tokenization platform.

### Install dependencies, build and tests

`mvn install`

### Run the adapter

`mvn spring-boot:run`

### Re-generate models and handlers

`./generate.sh`

