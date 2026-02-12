package com.dango.dangoaicodeapp.mapper;

import com.dango.dangoaicodeapp.model.entity.App;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 应用 映射层。
 *
 * @author dango
 */
public interface AppMapper extends BaseMapper<App> {

    /**
     * 游标分页查询应用列表
     *
     * @param tag        标签筛选
     * @param searchText 搜索关键词
     * @param lastId     游标（上一批最后一条的 id）
     * @param pageSize   每页数量
     * @return 应用列表
     */
    List<App> listAppByCursor(@Param("tag") String tag,
                              @Param("searchText") String searchText,
                              @Param("lastId") Long lastId,
                              @Param("pageSize") Integer pageSize);

    /**
     * 查询指定时间后更新的应用（包括已删除的）
     *
     * @param minUpdateTime 最小更新时间
     * @return 应用列表
     */
    List<App> listAppWithDelete(@Param("minUpdateTime") LocalDateTime minUpdateTime);
}
