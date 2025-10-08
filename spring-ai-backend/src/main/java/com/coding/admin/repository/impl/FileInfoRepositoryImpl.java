package com.coding.admin.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.coding.admin.mapper.FileInfoMapper;
import com.coding.admin.model.entity.FileInfoDO;
import com.coding.admin.repository.FileInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 文件信息Repository实现类
 */
@Repository
@RequiredArgsConstructor
public class FileInfoRepositoryImpl implements FileInfoRepository {

    private final FileInfoMapper fileInfoMapper;

    @Override
    public FileInfoDO getByMd5(String md5) {
        LambdaQueryWrapper<FileInfoDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FileInfoDO::getFileMd5, md5);
        return fileInfoMapper.selectOne(queryWrapper);
    }

    @Override
    public Long save(FileInfoDO fileInfoDO) {
        int result = fileInfoMapper.insert(fileInfoDO);
        return result > 0 ? fileInfoDO.getId() : null;
    }

    @Override
    public FileInfoDO getById(Long id) {
        return fileInfoMapper.selectById(id);
    }
}
