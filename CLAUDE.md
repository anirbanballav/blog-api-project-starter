# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Use the Maven wrapper from the repository root. It requires Java 25, which the pom sets as `java.version`.

- Run the app: `./mvnw spring-boot:run` (serves on port 8080)
- Build: `./mvnw clean package`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=BlogApiApplicationTests`
- Run a single test method: `./mvnw test -Dtest=BlogApiApplicationTests#contextLoads`

No lint or formatter is configured in the pom.

## Architecture

This is a single-module Spring Boot 4.0.5 web service (`spring-boot-starter-webmvc`) with package `com.embarkx.blogapi`.

The API is one controller, `BlogController`, mapped to `/api/posts`. Its state is a static in-memory `List<String>` that holds no persistence layer, so posts reset on every restart. Each post is stored as a single `title:content` string, and the post ID is its list index. `DELETE` and `GET /{id}` both use that index directly, which means deleting a post shifts the IDs of every later post. Requests with an out-of-range index currently surface as unhandled exceptions, since there is no error handling.

The endpoints return plain strings or lists, not JSON objects. `POST /validate` rejects content longer than 5000 characters with `Too long`. `GET /total` returns a hardcoded value (`100`, `200`, `300` concatenated) and does not compute anything from the stored posts.

Concurrency is not handled: the static list is a plain `ArrayList`, so concurrent requests can race.

`BlogApiApplication` is the standard `@SpringBootApplication` entry point. The only test is a `@SpringBootTest` context-load check, so there is no coverage of controller behavior.

A Postman collection, `BlogAPI.postman_collection.json`, at the repository root documents the endpoints for manual testing.
