package com.dgh06175.techblognotificationsserver;

import com.dgh06175.techblognotificationsserver.controller.TechBlogController;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class TechBlogNotificationsServerApplication {

    private final TechBlogController techBlogController;

    public TechBlogNotificationsServerApplication(TechBlogController techBlogController) {
        this.techBlogController = techBlogController;
    }

    public static void main(String[] args) {
        SpringApplication.run(TechBlogNotificationsServerApplication.class, args);
    }

    @Bean
    public CommandLineRunner runOnStartup() {
        return args -> {
            techBlogController.scrapPosts();
        };
    }
}
