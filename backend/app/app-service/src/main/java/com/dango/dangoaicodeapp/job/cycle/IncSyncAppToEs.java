package com.dango.dangoaicodeapp.job.cycle;

import cn.hutool.core.collection.CollUtil;
import com.dango.dangoaicodeapp.esdao.AppEsDao;
import com.dango.dangoaicodeapp.mapper.AppMapper;
import com.dango.dangoaicodeapp.model.entity.App;
import com.dango.dangoaicodeapp.model.es.AppEsDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 增量同步 App 到 ES
 * <p>
 * 每分钟同步最近 5 分钟内更新的数据
 *
 * @author dango
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "search.es.sync.incremental", havingValue = "true")
public class IncSyncAppToEs {

    @Resource
    private AppMapper appMapper;

    @Resource
    private AppEsDao appEsDao;

    @Scheduled(fixedRate = 60 * 1000)
    public void run() {
        // 查询最近 5 分钟更新的数据（包括已删除的）
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<App> appList = appMapper.listAppWithDelete(fiveMinutesAgo);

        if (CollUtil.isEmpty(appList)) {
            return;
        }

        List<AppEsDTO> esDTOList = appList.stream()
                .map(AppEsDTO::fromApp)
                .collect(Collectors.toList());

        // 分批写入
        final int batchSize = 500;
        int total = esDTOList.size();

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            appEsDao.saveAll(esDTOList.subList(i, end));
        }

        log.info("IncSyncAppToEs 完成，数量: {}", total);
    }
}
