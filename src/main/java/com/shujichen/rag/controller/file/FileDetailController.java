package com.shujichen.rag.controller.file;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.shujichen.rag.common.dto.Result;
import com.shujichen.rag.common.vo.file.FileDetailVO;
import com.shujichen.rag.service.FileDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件详情Controller
 */
@RestController
@RequestMapping("/api/file/fileDetail")
@RequiredArgsConstructor
@Tag(name = "文件记录表管理")
public class FileDetailController {

    private final FileDetailService fileDetailService;

    /**
     * 获取文件记录表列表
     *
     * @param page         页码
     * @param pageSize     每页数量
     * @param filename     文件名
     * @param hashInfo     哈希信息
     * @param uploadStatus 上传状态
     * @return 分页数据
     */
    @GetMapping("/list")
    @Operation(description = "获取文件记录表列表")
    public Result<IPage<FileDetailVO>> list(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                            @RequestParam(name = "filename", required = false) String filename,
                                            @RequestParam(name = "hashInfo", required = false) String hashInfo,
                                            @RequestParam(name = "uploadStatus", required = false) Integer uploadStatus) {
        return Result.success(fileDetailService.selectPage(page, pageSize, filename, hashInfo, uploadStatus));
    }

    /**
     * 获取文件记录表详情
     *
     * @param id 文件ID
     * @return 文件详情
     */
    @GetMapping("/{id}")
    @Operation(description = "获取文件记录表详情")
    public Result<FileDetailVO> getInfo(@PathVariable("id") Long id) {
        return Result.success(fileDetailService.getFileDetailVO(id));
    }

    /**
     * 删除文件记录表
     *
     * @param ids 文件ID列表
     * @return 是否成功
     */
    @DeleteMapping("/delete/{ids}")
    @Operation(description = "删除文件记录表")
    public Result<Boolean> remove(@PathVariable List<String> ids) {
        return Result.success(fileDetailService.removeFile(ids));
    }

    /**
     * 文件秒传检查
     *
     * @param md5 文件MD5值
     * @return 是否存在
     */
    @GetMapping("/check/{md5}")
    public Result<Boolean> checkFile(@PathVariable("md5") String md5) {
        try {
            return Result.success(fileDetailService.checkFile(md5));
        } catch (Exception e) {
            return Result.error("检查异常：" + e.getMessage());
        }
    }
}
