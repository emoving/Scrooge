package com.scrooge.scrooge.repository.community;

import com.scrooge.scrooge.domain.community.ArticleBad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ArticleBadRepository extends JpaRepository<ArticleBad, Long> {
    @Query("select ab from ArticleBad ab where ab.article.id = :articleId and ab.member.id = :memberId")
    ArticleBad findByArticleIdAndMemberId(Long articleId, Long memberId);

    @Query("SELECT COUNT(ab) from ArticleBad ab where ab.article.id = :articleId")
    Integer countByArticleId(Long articleId);
}
