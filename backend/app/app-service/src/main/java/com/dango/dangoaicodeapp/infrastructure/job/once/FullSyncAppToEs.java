package com.dango.dangoaicodeapp.infrastructure.job.once;

import cn.hutool.core.collection.CollUtil;
import com.dango.dangoaicodeapp.infrastructure.esdao.AppEsDao;
import com.dango.dangoaicodeapp.domain.app.entity.App;
import com.dango.dangoaicodeapp.infrastructure.esdao.AppEsDTO;
import com.dango.dangoaicodeapp.application.service.AppApplicationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全量同步 App 到 ES
 *
 * @author dango
 */
//@Component
@Slf4j
@ConditionalOnProperty(name = "search.es.sync.full-on-startup", havingValue = "true")
public class FullSyncAppToEs implements CommandLineRunner {

    @Resource
    private AppApplicationService appApplicationService;

    @Resource
    private AppEsDao appEsDao;

    @Override
    public void run(String... args) {
        List<App> appList = appApplicationService.list();
        if (CollUtil.isEmpty(appList)) {
            log.info("FullSyncAppToEs: 无数据需要同步");
            return;
        }

        List<AppEsDTO> esDTOList = appList.stream()
                .map(AppEsDTO::fromApp)
                .collect(Collectors.toList());

        // 分批写入，每批 500 条
        final int batchSize = 500;
        int total = esDTOList.size();
        log.info("FullSyncAppToEs 开始，总数: {}", total);

        for (int i = 0; i < total; i += batchSize) {
            int end = Math.min(i + batchSize, total);
            appEsDao.saveAll(esDTOList.subList(i, end));
            log.info("FullSyncAppToEs 进度: {}/{}", end, total);
        }

        log.info("FullSyncAppToEs 完成，总数: {}", total);
    }
}
