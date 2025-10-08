package com.coding.admin.repository;

import com.coding.admin.model.entity.FileInfoDO;

/**
 * 文件信息Repository接口
 */
public interface FileInfoRepository {

    /**
     * 根据MD5查询文件信息
     *
     * @param md5 文件MD5值
     * @return 文件信息
     */
    FileInfoDO getByMd5(String md5);

    /**
     * 保存文件信息
     *
     * @param fileInfoDO 文件信息
     * @return 文件ID
     */
    Long save(FileInfoDO fileInfoDO);

    /**
     * 根据ID查询文件信息
     *
     * @param id 文件ID
     * @return 文件信息
     */
    FileInfoDO getById(Long id);
}
