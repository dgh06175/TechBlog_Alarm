package com.dgh06175.techblognotificationsserver.controller;

import com.dgh06175.techblognotificationsserver.domain.Post;
import com.dgh06175.techblognotificationsserver.repository.AllPostRepository;
import com.dgh06175.techblognotificationsserver.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TechBlogController {

    private final PostRepository postRepository;
    private final AllPostRepository allPostRepository;

    @GetMapping("all-post")
    public List<Post> getAllPosts() {
        return allPostRepository.findAll();
    }

    @GetMapping("posts")
    public List<Post> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return postRepository.findAll(pageable).getContent();
    }
}
