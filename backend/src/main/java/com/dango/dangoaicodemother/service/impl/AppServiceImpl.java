package com.dango.dangoaicodemother.service.impl;

import com.dango.dangoaicodemother.mapper.AppMapper;
import com.dango.dangoaicodemother.model.entity.App;
import com.dango.dangoaicodemother.service.AppService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author dango
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService {

}
