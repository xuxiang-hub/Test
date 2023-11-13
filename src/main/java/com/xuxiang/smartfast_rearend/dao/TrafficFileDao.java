package com.xuxiang.smartfast_rearend.dao;

import com.xuxiang.smartfast_rearend.pojo.TrafficFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TrafficFileDao extends JpaRepository<TrafficFile, Integer> {

    // 查询最新的trafficFile对象，即id最大的trafficFile对象
    @Query("SELECT tfile FROM TrafficFile tfile WHERE tfile.id = (SELECT MAX(tfile2.id) FROM TrafficFile tfile2)")
    TrafficFile findLatestContract();
}
