package com.coding.core.repository;

import com.coding.core.model.entity.ResourceRecordDO;

/**
 * 文件信息Repository接口
 */
public interface ResourceRecordRepository {

    /**
     * 根据MD5查询文件信息
     *
     * @param md5 文件MD5值
     * @return 文件信息
     */
    ResourceRecordDO getByMd5(String md5);

    /**
     * 保存文件信息
     *
     * @param resourceRecordDO 文件信息
     * @return 文件ID
     */
    Long save(ResourceRecordDO resourceRecordDO);

    /**
     * 根据ID查询文件信息
     *
     * @param id 文件ID
     * @return 文件信息
     */
    ResourceRecordDO getById(Long id);
}
