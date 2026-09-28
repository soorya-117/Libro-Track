package com.example.librotrack.repository;

import com.example.librotrack.entity.IssueRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long> {

    List<IssueRecord> findByStudentIdAndReturnDateIsNull(Long studentId);
}
