package com.opslab.blog.admin.controller;

import com.opslab.blog.admin.APP;
import com.opslab.blog.admin.webentity.UIResult;
import com.opslab.blog.admin.domain.UploadGroup;
import com.opslab.blog.admin.domain.UploadInfo;
import com.opslab.blog.admin.constant.UploadInfoStatus;
import com.opslab.blog.admin.executor.UploadFileExecutor;
import com.opslab.blog.admin.service.UploadGroupService;
import com.opslab.blog.admin.service.UploadInfoService;
import com.opslab.blog.util.FileUtil;
import com.opslab.blog.util.UnitUtil;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传
 */
@Controller
@RequestMapping("/file")
public class FileUploadController {
    private static Logger logger = LogManager.getLogger(FileUploadController.class);

    @Autowired
    private UploadInfoService uploadInfoService;

    @Autowired
    private UploadGroupService uploadGroupService;

    @Autowired
    private UploadFileExecutor fileExecutor;


    /**
     * 当文件上传
     *
     * @param gid  上传组id
     * @param bid  业务系统业务主键
     * @param file 上传的文件
     * @return
     */
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public UIResult uploadFile(@RequestParam("gid") int gid, @RequestParam("bid") String bid, @RequestParam("file") MultipartFile file) {
        if (gid == 0 || bid == null || "".equals(bid)) {
            return UIResult.error("params error!");
        }
        UploadGroup imageGroup = uploadGroupService.imageGroup(gid);
        if (imageGroup == null) {
            return UIResult.error("gid error!");
        }
        logger.info("upload file:"+gid+"=>"+bid+"=>"+file.getName());
        String basePath = APP.UPLOAD_BASE_PATH + File.separator + imageGroup.getPath()
                + File.separator + imageGroup.getOriginalPath() + File.separator;

        if (!file.isEmpty()) {
            String id = FileUtil.UUID();
            String oldName = file.getOriginalFilename();
            String newName = id + "." +  FileUtil.suffix(file.getOriginalFilename());
            String fileName = FileUtil.path(basePath + File.separator + newName);
            UploadInfoStatus uploadStatus = UploadInfoStatus.CREATE;
            String uploadStatusMsg = "";
            String md5 = "";
            String sha1 = "";
            try {
                md5 = FileUtil.inputHash(file.getInputStream(), "MD5");
                sha1 = FileUtil.inputHash(file.getInputStream(), "SHA1");
                if (file.getSize() > imageGroup.getLimit()) {
                    uploadStatus = UploadInfoStatus.ILLEGAL;
                    uploadStatusMsg = "The file is too large";
                    return UIResult.error("The file is too large!");
                }
                file.transferTo(new File(fileName));
                fileExecutor.hand(new File(fileName));
                uploadStatus = UploadInfoStatus.SUCCESS;
                return UIResult.success("upload success!", newName);
            } catch (Exception e) {
                uploadStatus = UploadInfoStatus.ERROR;
                return UIResult.error("upload error!", e.getMessage());
            } finally {
                UploadInfo uploadInfo =
                        new UploadInfo(id, gid, bid, oldName, newName, uploadStatus, uploadStatusMsg,
                                fileName, md5, sha1, file.getSize());
                uploadInfoService.save(uploadInfo);
            }
        }
        return UIResult.error("upload error!");
    }



    /**
     * 多文件上传
     * @param gid 上传文件组
     * @param bid 业务系统id
     * @param files 上传文件组
     * @return
     */
    @RequestMapping(value = "/uploads", method = RequestMethod.POST)
    @ResponseBody
    public UIResult uploadFiles(@RequestParam("gid") int gid, @RequestParam("bid") String bid,
                                @RequestParam("file") MultipartFile[] files) {
        if (gid == 0 || bid == null || "".equals(bid)) {
            return UIResult.error("params error!");
        }
        UploadGroup imageGroup = uploadGroupService.imageGroup(gid);
        if (imageGroup == null) {
            return UIResult.error("gid error!");
        }

        logger.info("upload file:"+gid+"=>"+bid+"=>"+files.length);
        //保存原文件的路径
        String basePath = FileUtil.path(APP.UPLOAD_BASE_PATH + File.separator + imageGroup.getPath()
                + File.separator + imageGroup.getOriginalPath() + File.separator);
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (!file.isEmpty()) {
                String id = FileUtil.UUID();
                String oldName = file.getOriginalFilename();
                String newName = id + "." + FileUtil.suffix(file.getOriginalFilename());
                String fileName = FileUtil.path(basePath + File.separator + newName);
                UploadInfoStatus uploadStatus = UploadInfoStatus.CREATE;
                String uploadStatusMsg = "";
                String md5 = "";
                String sha1 = "";
                String resultMsg = "";
                try {
                    md5 = FileUtil.inputHash(file.getInputStream(), "MD5");
                    sha1 = FileUtil.inputHash(file.getInputStream(), "SHA1");
                    if (file.getSize() > imageGroup.getLimit()) {
                        uploadStatus = UploadInfoStatus.ILLEGAL;
                        uploadStatusMsg = "The file is too large";
                        resultMsg = "The file is too large! file size limit:" + UnitUtil.B2M(imageGroup.getLimit()) + "M";
                    }
                    if (!FileUtil.isLegalSuffix(imageGroup.getSuffixs(), oldName)) {
                        uploadStatus = UploadInfoStatus.ILLEGAL;
                        uploadStatusMsg = "Illegal file";
                        resultMsg = "Illegal file,it's Only support:" + imageGroup.getSuffixs();
                    }
                    file.transferTo(new File(fileName));
                    fileExecutor.hand(new File(fileName));
                    uploadStatus = UploadInfoStatus.SUCCESS;
                    resultMsg = "upload success!";
                } catch (Exception e) {
                    uploadStatus = UploadInfoStatus.ERROR;
                    resultMsg = "upload error!";
                } finally {
                    Map<String, String> resultMap = new HashMap<>();
                    resultMap.put("status", String.valueOf(uploadStatus));
                    resultMap.put("msg", resultMsg);
                    if (UploadInfoStatus.SUCCESS == uploadStatus) {
                        resultMap.put("file", newName);
                    }
                    map.put(oldName, resultMap);
                    UploadInfo uploadInfo =
                            new UploadInfo(id, gid, bid, oldName, newName, uploadStatus, uploadStatusMsg,
                                    fileName, md5, sha1, file.getSize());
                    uploadInfoService.save(uploadInfo);
                }
            }
        }
        return UIResult.success("uploadSuccess", map);
    }
}
