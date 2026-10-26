package com.aes.answersheets;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerPageRepository extends JpaRepository<AnswerPage, Long> {

    List<AnswerPage> findByAnswerSheetIdOrderByPageNumberAsc(Long answerSheetId);

    Optional<AnswerPage> findByAnswerSheetIdAndPageNumber(Long answerSheetId, Integer pageNumber);
}