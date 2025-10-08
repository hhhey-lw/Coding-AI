package com.coding.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.coding.admin.model.entity.ProviderConfigDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 提供商配置Mapper
 */
@Mapper
public interface ProviderConfigMapper extends BaseMapper<ProviderConfigDO> {

    /**
     * 根据提供商代码和服务类型查询配置
     */
    ProviderConfigDO selectByProviderCodeAndServiceType(@Param("providerCode") String providerCode,
                                                       @Param("serviceType") String serviceType);
}
