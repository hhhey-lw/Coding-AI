package com.coding.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.core.model.entity.AiProviderConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 提供商配置Mapper
 */
@Mapper
public interface AiProviderConfigMapper extends BaseMapper<AiProviderConfigDO> {

    /**
     * 根据提供商代码和服务类型查询配置
     */
    AiProviderConfigDO selectByProviderCodeAndServiceType(@Param("providerCode") String providerCode,
                                                          @Param("serviceType") String serviceType);
}
