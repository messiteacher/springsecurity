package com.example.securityExam;

import com.example.securityExam.domain.member.member.entity.Member;
import com.example.securityExam.domain.member.member.service.MemberService;
import com.example.securityExam.domain.post.post.controller.ApiV1PostController;
import com.example.securityExam.domain.post.post.entity.Post;
import com.example.securityExam.domain.post.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class ApiV1PostControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostService postService;

    @Autowired
    private MemberService memberService;

    private void checkPost(ResultActions resultActions, Post post) throws Exception {

        resultActions.andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(post.getId()))
                .andExpect(jsonPath("$.data.title").value(post.getTitle()))
                .andExpect(jsonPath("$.data.content").value(post.getContent()))
                .andExpect(jsonPath("$.data.authorId").value(post.getAuthor().getId()))
                .andExpect(jsonPath("$.data.authorName").value(post.getAuthor().getNickname()))
                .andExpect(jsonPath("$.data.published").value(post.isPublished()))
                .andExpect(jsonPath("$.data.listed").value(post.isListed()))
                .andExpect(jsonPath("$.data.createdDate").value(matchesPattern(post.getCreatedDate().toString().replaceAll("0+$", "") + ".*")))
                .andExpect(jsonPath("$.data.modifiedDate").value(matchesPattern(post.getModifiedDate().toString().replaceAll("0+$", "") + ".*")));
    }

    private void checkPosts(List<Post> posts, ResultActions resultActions) throws Exception {

        for(int i = 0; i < posts.size(); i++) {

            Post post = posts.get(i);

            resultActions
                    .andExpect(jsonPath("$.data.items[%d]".formatted(i)).exists())
                    .andExpect(jsonPath("$.data.items[%d].id".formatted(i)).value(post.getId()))
                    .andExpect(jsonPath("$.data.items[%d].title".formatted(i)).value(post.getTitle()))
                    .andExpect(jsonPath("$.data.items[%d].content".formatted(i)).doesNotExist())
                    .andExpect(jsonPath("$.data.items[%d].authorId".formatted(i)).value(post.getAuthor().getId()))
                    .andExpect(jsonPath("$.data.items[%d].authorName".formatted(i)).value(post.getAuthor().getNickname()))
                    .andExpect(jsonPath("$.data.items[%d].published".formatted(i)).value(post.isPublished()))
                    .andExpect(jsonPath("$.data.items[%d].listed".formatted(i)).value(post.isListed()))
                    .andExpect(jsonPath("$.data.items[%d].createdDate".formatted(i)).value(matchesPattern(post.getCreatedDate().toString().replaceAll("0+$", "") + ".*")))
                    .andExpect(jsonPath("$.data.items[%d].modifiedDate".formatted(i)).value(matchesPattern(post.getModifiedDate().toString().replaceAll("0+$", "") + ".*")));
        }
    }

    private ResultActions writeRequest(String apiKey, String title, String content) throws Exception {

        ResultActions resultActions = mvc.perform(
                        post("/api/v1/posts")
                                .header("Authorization", "Bearer " + apiKey)
                                .content("""
                                        {
                                            "title": "%s",
                                            "content": "%s",
                                            "published": true,
                                            "listed": true
                                        }
                                        """.formatted(title, content)
                                        .stripIndent())
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());

        return resultActions;
    }

    private ResultActions itemRequest(long postId, String apiKey) throws Exception {

        return mvc.perform(
                        get("/api/v1/posts/%d".formatted(postId))
                                .header("Authorization", "Bearer " + apiKey)
                )
                .andDo(print());
    }

    private ResultActions modifyRequest(long postId, String apiKey, String title, String content) throws Exception {

        return mvc.perform(
                        put("/api/v1/posts/%d".formatted(postId))
                                .header("Authorization", "Bearer " + apiKey)
                                .content("""
                                        {
                                            "title": "%s",
                                            "content": "%s",
                                            "published": true,
                                            "listed": true
                                        }
                                        """.formatted(title, content)
                                        .stripIndent())
                                .contentType(
                                        new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
                                )
                )
                .andDo(print());
    }

    @Test
    @DisplayName("글 다건 조회")
    void items1() throws Exception {

        ResultActions resultActions = mvc.perform(
                        get("/api/v1/posts")
                )
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("글 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items.length()").value( 3))
                .andExpect(jsonPath("$.data.currentPageNo").isNumber())
                .andExpect(jsonPath("$.data.totalPages").isNumber());

        Page<Post> postPage = postService.getListedItems(1, 3, "title", "");
        List<Post> posts = postPage.getContent();

        checkPosts(posts, resultActions);
    }

    @Test
    @DisplayName("글 다건 조회 - 검색 - 제목, 페이징이 되어야 함.")
    void items2() throws Exception {

        int page = 1;
        int pageSize = 3;
        String keywordType = "title";
        String keyword = "title";

        ResultActions resultActions = mvc.perform(
                        get("/api/v1/posts?page=%d&pageSize=%d&keywordType=%s&keyword=%s"
                                .formatted(page, pageSize, keywordType, keyword)))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("글 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items.length()").value( pageSize))
                .andExpect(jsonPath("$.data.currentPageNo").value(page))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.totalItems").value(7));

        Page<Post> postPage = postService.getListedItems(page, pageSize , keywordType, keyword);
        List<Post> posts = postPage.getContent();

        checkPosts(posts, resultActions);
    }

    @Test
    @DisplayName("글 다건 조회 - 검색 - 내용, 페이징이 되어야 함.")
    void items3() throws Exception {

        int page = 1;
        int pageSize = 3;
        String keywordType = "content";
        String keyword = "content";

        ResultActions resultActions = mvc.perform(
                        get("/api/v1/posts?page=%d&pageSize=%d&keywordType=%s&keyword=%s"
                                .formatted(page, pageSize, keywordType, keyword)))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItems"))
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("글 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items.length()").value(pageSize))
                .andExpect(jsonPath("$.data.currentPageNo").value(page))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.totalItems").value(7));

        Page<Post> postPage = postService.getListedItems(page, pageSize , keywordType, keyword);
        List<Post> posts = postPage.getContent();

        checkPosts(posts, resultActions);
    }

    @Test
    @DisplayName("내가 작성한 글 조회 (user1) - 검색, 페이징이 되어야 함.")
    void mines() throws Exception {

        int page = 1;
        int pageSize = 3;
        String keywordType = "";
        String keyword = "";
        String apiKey = "user1";

        ResultActions resultActions = mvc.perform(
                        get("/api/v1/posts/mine?page=%d&pageSize=%d&keywordType=%s&keyword=%s"
                                .formatted(page, pageSize, keywordType, keyword))
                                .header("Authorization", "Bearer " + apiKey))
                .andDo(print());

        resultActions.andExpect(status().isOk())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getMines"))
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("내 글 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items.length()").value(pageSize))
                .andExpect(jsonPath("$.data.currentPageNo").value(page))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.totalItems").value(4));

        Member author = memberService.findByApiKey(apiKey).get();
        Page<Post> postPage = postService.getMines(author, page, pageSize , keywordType, keyword);
        List<Post> posts = postPage.getContent();

        checkPosts(posts, resultActions);
    }

    @Test
    @DisplayName("글 단건 조회 1 - 다른 유저의 공개글 조회")
    void item1() throws Exception {

        long postId = 1;
        String apiKey = "";

        ResultActions resultActions = itemRequest(postId, apiKey);

        resultActions.andExpect(status().isOk())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글을 조회하였습니다.".formatted(postId)));

        Post post = postService.getItem(postId).get();
        checkPost(resultActions, post);
    }

    @Test
    @DisplayName("글 단건 조회 2 - 없는 글 조회")
    void item2() throws Exception {

        long postId = 100000;
        String apiKey = "user2";

        ResultActions resultActions = itemRequest(postId, apiKey);

        resultActions.andExpect(status().isNotFound())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(jsonPath("$.code").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 글입니다."));
    }

    @Test
    @DisplayName("글 단건 조회 3 - 다른 유저의 비공개 글 조회")
    void item3() throws Exception {

        long postId = 3;
        String apiKey = "user1";

        ResultActions resultActions = itemRequest(postId, apiKey);

        resultActions.andExpect(status().isForbidden())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("getItem"))
                .andExpect(jsonPath("$.code").value("403-1"))
                .andExpect(jsonPath("$.msg").value("비공개 설정된 글입니다."));
    }

    @Test
    @DisplayName("글 작성")
    void write1() throws Exception {

        String apiKey = "user1";
        String title = "새로운 글 제목";
        String content = "새로운 글 내용";

        ResultActions resultActions = writeRequest(apiKey, title, content);

        Post post = postService.getLatestItem().get();

        resultActions.andExpect(status().isCreated())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(jsonPath("$.code").value("201-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글 작성이 완료되었습니다.".formatted(post.getId())));

        checkPost(resultActions, post);
    }

    @Test
    @DisplayName("글 작성2 - no apiKey")
    void write2() throws Exception {

        String apiKey = "";
        String title = "새로운 글 제목";
        String content = "새로운 글 내용";

        ResultActions resultActions = writeRequest(apiKey, title, content);

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401-1"))
                .andExpect(jsonPath("$.msg").value("잘못된 인증키입니다."));
    }

    @Test
    @DisplayName("글 작성3 - no input data")
    void write3() throws Exception {

        String apiKey = "user1";
        String title = "";
        String content = "";

        ResultActions resultActions = writeRequest(apiKey, title, content);

        resultActions.andExpect(status().isBadRequest())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("write"))
                .andExpect(jsonPath("$.code").value("400-1"))
                .andExpect(jsonPath("$.msg").value("""
                        content : NotBlank : must not be blank
                        title : NotBlank : must not be blank
                        """.trim().stripIndent()));
    }

    @Test
    @DisplayName("글 수정")
    void modify1() throws Exception {

        long postId = 1;
        String apiKey = "user1";
        String title = "수정된 글 제목";
        String content = "수정된 글 내용";

        ResultActions resultActions = modifyRequest(postId, apiKey, title, content);

        resultActions.andExpect(status().isOk())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글 수정이 완료되었습니다.".formatted(postId)));

        Post post = postService.getItem(postId).get();
        checkPost(resultActions, post);
    }

    @Test
    @DisplayName("글 수정 2 - no apiKey")
    void modify2() throws Exception {

        long postId = 1;
        String apiKey = "123123123";
        String title = "수정된 글 제목";
        String content = "수정된 글 내용";

        ResultActions resultActions = modifyRequest(postId, apiKey, title, content);

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401-1"))
                .andExpect(jsonPath("$.msg").value("잘못된 인증키입니다."));
    }

    @Test
    @DisplayName("글 수정 3 - no input data")
    void modify3() throws Exception {

        long postId = 1;
        String apiKey = "user1";
        String title = "";
        String content = "";

        ResultActions resultActions = modifyRequest(postId, apiKey, title, content);

        resultActions.andExpect(status().isBadRequest())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(jsonPath("$.code").value("400-1"))
                .andExpect(jsonPath("$.msg").value("""
                        content : NotBlank : must not be blank
                        title : NotBlank : must not be blank
                        """.trim().stripIndent()));
    }

    @Test
    @DisplayName("글 수정 4 - no permission")
    void modify4() throws Exception {

        long postId = 1;
        String apiKey = "user2";
        String title = "다른 유저의 글 제목 수정";
        String content = "다른 유저의 글 내용 수정";

        ResultActions resultActions = modifyRequest(postId, apiKey, title, content);

        resultActions.andExpect(status().isForbidden())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("modify"))
                .andExpect(jsonPath("$.code").value("403-1"))
                .andExpect(jsonPath("$.msg").value("자신이 작성한 글만 수정 가능합니다."));
    }

    private ResultActions deleteRequest(long postId, String apiKey) throws Exception {

        return mvc.perform(
                        delete("/api/v1/posts/%d".formatted(postId))
                                .header("Authorization", "Bearer " + apiKey))
                .andDo(print());
    }

    @Test
    @DisplayName("글 삭제")
    void delete1() throws Exception {

        long postId = 1;
        String apiKey = "user1";

        ResultActions resultActions = deleteRequest(postId, apiKey);

        resultActions.andExpect(status().isOk())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글 삭제가 완료되었습니다.".formatted(postId)));
    }

    @Test
    @DisplayName("글 삭제 - no apiKey")
    void delete2() throws Exception {

        long postId = 1;
        String apiKey = "sdfsdfsd";

        ResultActions resultActions = deleteRequest(postId, apiKey);

        resultActions.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401-1"))
                .andExpect(jsonPath("$.msg").value("잘못된 인증키입니다."));
    }

    @Test
    @DisplayName("글 삭제 - no permission")
    void delete3() throws Exception {

        long postId = 1;
        String apiKey = "user2";

        ResultActions resultActions = deleteRequest(postId, apiKey);

        resultActions.andExpect(status().isForbidden())
                .andExpect(handler().handlerType(ApiV1PostController.class))
                .andExpect(handler().methodName("delete"))
                .andExpect(jsonPath("$.code").value("403-1"))
                .andExpect(jsonPath("$.msg").value("자신이 작성한 글만 삭제 가능합니다."));
    }
}
