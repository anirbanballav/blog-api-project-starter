
---
name: explain-code
description: Explains the blog-api Spring Boot project end to end, covering its structure, classes, endpoints, data model, validation rules, error handling, tests, build commands, and known limitations. Use when the user asks what the code does, how it is organized, or how a specific endpoint or class works.
---

# Explain the blog-api codebase

This skill describes the whole `blog-api` project. Use it to answer questions about how the code works. Read the source files again when the user asks about a detail that may have changed.

## Executive summary

The project is a single-module Spring Boot 4.0.5 web service in package `com.embarkx.blogapi`. It exposes one REST controller under `/api/posts`. Posts are held in a static in-memory list, so all data is lost on restart. The API returns plain strings and lists rather than JSON objects.

## Project layout

```
pom.xml                                   Maven build, Spring Boot parent 4.0.5, Java 25
mvnw, mvnw.cmd, .mvn/wrapper/             Maven wrapper scripts
src/main/java/com/embarkx/blogapi/
    BlogApiApplication.java               Spring Boot entry point
    BlogController.java                   All endpoints, validation, storage, error handling
    Post.java                             Lombok data class with title and content
src/main/resources/application.properties Sets spring.application.name=blog-api
src/test/java/com/embarkx/blogapi/
    BlogApiApplicationTests.java          Single context-load test
BlogAPI.postman_collection.json           Postman collection for manual endpoint testing
CLAUDE.md, README.md                      Project guidance and documentation
```

## Build and run

The project requires Java 25. Use the Maven wrapper from the repository root.

```bash
./mvnw spring-boot:run          # start on port 8080
./mvnw clean package            # build
./mvnw test                     # run all tests
./mvnw test -Dtest=BlogApiApplicationTests#contextLoads   # run one test method
```

Dependencies are `spring-boot-starter-webmvc`, `spring-boot-devtools`, and `lombok`. The test dependency is `spring-boot-starter-webmvc-test`. No lint or formatter is configured.

## Entry point: BlogApiApplication

`BlogApiApplication` is a standard `@SpringBootApplication` class. Its `main` method calls `SpringApplication.run`. Component scanning starts at the package `com.embarkx.blogapi`, so `BlogController` and `Post` are discovered from it.

## Data model: Post

`Post` is a Lombok class annotated with `@Data`, `@NoArgsConstructor`, and `@AllArgsConstructor`. It has two `String` fields, `title` and `content`. Spring uses it to bind the JSON request body of `POST /api/posts`. The controller never stores a `Post` object. It reads the two fields, checks them, and saves a combined string.

## Storage model

`BlogController` keeps posts in `private static List<String> posts = new ArrayList<>()`. Each post is stored as a single string in the form `title:content`. A post's ID is its index in this list.

Three consequences follow from this design:

1. Data does not persist. Restarting the application clears all posts.
2. Deleting a post shifts the IDs of every later post, because removing an element from a list moves the remaining elements down.
3. The `static` list is a plain `ArrayList`, which is not thread-safe. Concurrent requests can race.

## Constants

| Constant | Value | Used for |
|---|---|---|
| `MAX_TITLE_LENGTH` | 50 | Maximum title length on create |
| `MAX_CONTENT_LENGTH` | 200 | Maximum content length on create |
| `MAX_VALIDATE_LENGTH` | 5000 | Maximum content length on the validate endpoint |

## Endpoints

All routes are relative to `/api/posts`.

### POST /api/posts: create a post

The request body is JSON with `title` and `content`. The controller validates them in this order and returns `400 Bad Request` with a plain-text message on the first failure:

1. Title is null or blank: `Title must not be blank`
2. Title is longer than 50 characters: `Title must not exceed 50 characters`
3. Content is null or blank: `Content must not be blank`
4. Content is longer than 200 characters: `Content must not exceed 200 characters`

On success, it stores `title + ":" + content` and returns `200 OK` with the body `Post created`.

### GET /api/posts: list all posts

Returns the raw list of stored `title:content` strings as a JSON array. An empty list is returned when no posts exist.

### GET /api/posts/{id}: get one post

Returns the stored string at index `id`. The index is a zero-based `int` path variable. An out-of-range index throws `IndexOutOfBoundsException`, which the controller maps to a 404 (see error handling below).

### DELETE /api/posts/{id}: delete one post

Removes the element at index `id` and returns the plain-text body `Deleted!!!`. An out-of-range index causes the same 404 response. Later posts move to lower IDs after a successful delete.

### POST /api/posts/validate?content=...: check content length

Takes `content` as a request parameter, not in the body. It returns `Too long` if the length is greater than 5000 characters, and `OK` otherwise. It does not store anything. If the parameter is missing, Spring returns a 400 response before the method runs.

### GET /api/posts/total: count words

Returns `Total words: N`. The method loops over every stored post, replaces the `:` separator with a space, trims the result, and splits it on whitespace. Each non-empty piece counts as one word. The count includes both the title and the content.

Note that the project guidance in `CLAUDE.md` says this endpoint returns a hardcoded value. That is out of date. The current code calculates the total from the stored posts.

## Error handling

`BlogController` contains one `@ExceptionHandler` for `IndexOutOfBoundsException`. It returns `404 Not Found` with the body `Post not found. Please check the post ID and try again.` This covers both `GET /{id}` and `DELETE /{id}`.

No other errors are handled. For example, a missing or unreadable JSON body on create causes a standard Spring 400 response. Deleting or reading an invalid ID returns the 404 message above.

## Tests

`BlogApiApplicationTests` contains one `@SpringBootTest` method, `contextLoads`, which starts the application context and checks that it loads. No controller behavior is tested. Adding `@WebMvcTest` or `MockMvc` tests would be the natural next step.

## Known limitations

1. No persistence. All posts are lost on restart.
2. Deleting shifts the IDs of later posts, so a client that caches an ID can read or delete the wrong post.
3. The static list is not thread-safe.
4. Posts are stored as one `title:content` string. Titles that contain a colon are split ambiguously by the word-count logic, although the count still works because the colon becomes a space.
5. No authentication, authorization, or rate limiting.
6. The `Post` class is used only for request binding. Responses are plain strings, not JSON objects, so there is no structured response model.
7. The validate endpoint duplicates the length rule with a different limit (5000) from create (200 for content). The two limits are not shared.

## Manual testing

`BlogAPI.postman_collection.json` at the repository root contains requests for each endpoint. Import it into Postman to test the API against a running instance on port 8080.