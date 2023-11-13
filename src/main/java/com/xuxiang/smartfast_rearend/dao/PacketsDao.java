package com.xuxiang.smartfast_rearend.dao;

import com.xuxiang.smartfast_rearend.pojo.Packets;
import com.xuxiang.smartfast_rearend.pojo.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PacketsDao extends JpaRepository<Packets, Integer> {
    // 根据session_id找出所有packets
    @Query("SELECT p FROM Packets p WHERE p.session.sessionId = :sessionId")
    List<Packets> findBySessionId(@Param("sessionId") int sessionId);
}
