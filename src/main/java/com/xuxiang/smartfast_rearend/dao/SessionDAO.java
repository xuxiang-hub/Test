package com.xuxiang.smartfast_rearend.dao;

import com.xuxiang.smartfast_rearend.pojo.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SessionDAO extends JpaRepository<Session, Integer> {
    // 根据traffic_id找出所有session
    @Query("SELECT s FROM Session s WHERE s.trafficFile.id = :trafficId")
    List<Session> findByTrafficId(@Param("trafficId") int trafficId);
}
