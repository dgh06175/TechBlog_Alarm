package com.dgh06175.techblognotificationsserver.service;


import com.dgh06175.techblognotificationsserver.config.BlogConfig;
import com.dgh06175.techblognotificationsserver.config.html.Inflab;
import com.dgh06175.techblognotificationsserver.config.html.KakaoBank;
import com.dgh06175.techblognotificationsserver.config.html.Toss;
import com.dgh06175.techblognotificationsserver.config.rss.Kakao;
import com.dgh06175.techblognotificationsserver.config.rss.Woowahan;
import com.dgh06175.techblognotificationsserver.domain.Post;
import com.dgh06175.techblognotificationsserver.exception.ErrorMessage;
import com.dgh06175.techblognotificationsserver.exception.ScrapException;
import com.dgh06175.techblognotificationsserver.exception.ScrapParsingException;
import com.dgh06175.techblognotificationsserver.repository.PostRepository;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    public List<Post> findAll(int page, int size) {
        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(Sort.Direction.DESC, "pubDate"));
        org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        return postRepository.findAll(pageable).getContent();
    }

    public List<BlogConfig> getBlogConfigs() {
        List<BlogConfig> tmpConfigs = new ArrayList<>();
        tmpConfigs.add(new Toss());
        tmpConfigs.add(new Woowahan());
        tmpConfigs.add(new Kakao());
        tmpConfigs.add(new Inflab());
        tmpConfigs.add(new KakaoBank());

        return tmpConfigs;
    }

    public List<Post> scrapPosts(BlogConfig blogConfig) throws ScrapException {
        int MAX_RETRIES = 3;
        long INITIAL_RETRY_DELAY_SEC = 2;
        long MAX_RETRY_DELAY_SEC = 3;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return parse(blogConfig);
            } catch (ScrapException e) {
                log.warn("시도 횟수 {} 실패: {}", attempt, e.getMessage());

                if (attempt >= MAX_RETRIES) {
                    log.error("재시도 실패");
                    throw e;
                }

                // 지수 백오프: 재시도 간격을 두 배로 증가시킨다.
                long delay = Math.min(INITIAL_RETRY_DELAY_SEC * (long) Math.pow(2, attempt - 1), MAX_RETRY_DELAY_SEC);

                try {
                    TimeUnit.SECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ScrapException(ie.toString(), ErrorMessage.THREAD_INTERRUPT_EXCEPTION);
                }
            }
        }

        throw new ScrapException("", ErrorMessage.UNEXPECTED_RETRY_CONDITION_EXCEPTION);
    }

    private List<Post> parse(BlogConfig blogConfig) throws ScrapException {
        List<Post> scrapedPosts = new ArrayList<>();
        try {
            Document document = Jsoup.connect(blogConfig.getBlogUrl()).get();
            Elements items = document.select(blogConfig.getListTagName());
            if (items.isEmpty()) {
                throw new ScrapParsingException(blogConfig.getBlogUrl());
            } else {
                for (Element item : items) {
                    Post post = blogConfig.parseElement(item);
                    if (post.getTitle().isEmpty() || post.getLink().equals(blogConfig.getBlogUrl())) {
                        post.print();
                        throw new ScrapParsingException(blogConfig.getBlogUrl());
                    }
                    scrapedPosts.add(post);
                }
            }
        } catch (UnknownHostException e) {
            log.warn("블로그 {}에 연결할 수 없음: {}", blogConfig.getBlogName(), e.getMessage());
        } catch (IOException e) {
            log.warn("블로그 {}에서 IO 예외 발생: {}", blogConfig.getBlogName(), e.getMessage());
        } catch (ScrapParsingException e) {
            log.warn("블로그 {}에서 파싱 예외 발생: {}", blogConfig.getBlogName(), e.getMessage());
        }
        return scrapedPosts;
    }


    public void savePosts(List<Post> scrapedPosts) {
        log.info("스크랩한 게시글 개수: {}\n", scrapedPosts.size());
        List<String> scrapedLinks = scrapedPosts.stream()
                .map(Post::getLink)
                .toList();

        List<String> duplicateLinks = postRepository.findAllByLinkIn(scrapedLinks)
                .stream()
                .map(Post::getLink)
                .toList();

        
        log.info("겹치는 게시글 개수: {}\n", duplicateLinks.size());

        List<Post> newPosts = scrapedPosts.stream()
                .filter(post -> !duplicateLinks.contains(post.getLink()))
                .toList();

        log.info("새로운 게시글 개수: {}\n", newPosts.size());

        if (newPosts.isEmpty()) {
            log.info("저장할 새로운 포스트가 없습니다.");
            return;
        }
        printPosts(newPosts);
        saveNewPosts(newPosts);
    }

    private void printPosts(List<Post> posts) {
        for (Post post : posts) {
            log.info("\n포스트 저장됨: {}", post.getLink());
            post.print();
        }
    }

    private void saveNewPosts(List<Post> newPosts) {
        postRepository.saveAll(newPosts);
        log.info("{} 개의 포스트 저장됨\n", newPosts.size());
    }

    public void printMissingPostsCount(List<Post> scrapedPosts) {
        // 스크랩한 게시글의 링크 목록을 추출
        List<String> scrapedLinks = scrapedPosts.stream()
                .map(Post::getLink)
                .toList();

        // 데이터베이스에 있는 모든 게시글의 링크를 조회
        List<String> allLinksInDb = postRepository.findAll()
                .stream()
                .map(Post::getLink)
                .toList();

        // 데이터베이스에는 있지만 스크랩 결과에 없는 게시글의 링크 목록
        List<String> missingLinks = allLinksInDb.stream()
                .filter(link -> !scrapedLinks.contains(link))
                .toList();

        // 결과 출력
        for (var missingLink : missingLinks) {
            log.info(missingLink);
        }
        log.info("스크랩 결과에 없는 게시글 개수: {}\n", missingLinks.size());
    }
}
