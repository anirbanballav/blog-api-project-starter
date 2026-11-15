package com.embarkx.blogapi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class BlogController {

    private static List<String> posts = new ArrayList<>();

    private static final int MAX_TITLE_LENGTH = 50;
    private static final int MAX_CONTENT_LENGTH = 200;
    private static final int MAX_VALIDATE_LENGTH = 5000;

    @PostMapping
    public ResponseEntity<String> createPost(@RequestBody Post request) {
        String title = request.getTitle();
        String content = request.getContent();
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body("Title must not be blank");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            return ResponseEntity.badRequest().body("Title must not exceed " + MAX_TITLE_LENGTH + " characters");
        }
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body("Content must not be blank");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            return ResponseEntity.badRequest().body("Content must not exceed " + MAX_CONTENT_LENGTH + " characters");
        }
        String entry = title + ":" + content;
        posts.add(entry);
        return ResponseEntity.ok("Post created");
    }


    @GetMapping
    public List<String> getAllPosts() {
        return posts;
    }

    @GetMapping("/{id}")
    public String getPost(@PathVariable int id) {
        return posts.get(id);
    }

    @PostMapping("/validate")
    public String validateContent(@RequestParam String content) {
        if (content.length() > MAX_VALIDATE_LENGTH) {
            return "Too long";
        }
        return "OK";
    }

    @ExceptionHandler(IndexOutOfBoundsException.class)
    public ResponseEntity<String> handleIndexOutOfBounds(IndexOutOfBoundsException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Post not found. Please check the post ID and try again.");
    }

    @DeleteMapping("/{id}")
    public String deletePost(@PathVariable int id) {
        posts.remove(id);
        return "Deleted!!!";
    }

    @GetMapping("/total")
    public String getTotalWordCount() {
        long total = 0;
        for (String post : posts) {
            // Stored posts are "title:content"; treat the separator as whitespace
            String text = post.replace(':', ' ').trim();
            if (!text.isEmpty()) {
                total += text.split("\\s+").length;
            }
        }
        return "Total words: " + total;
    }
}