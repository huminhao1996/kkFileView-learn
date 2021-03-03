package cn.keking.service.impl;

import cn.keking.config.ConfigConstants;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import cn.keking.service.FilePreview;
import cn.keking.utils.DownloadUtils;
import cn.keking.utils.FileUtils;
import cn.keking.utils.PdfUtils;
import cn.keking.web.filter.BaseUrlFilter;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;

/**
 * Created by kl on 2018/1/17.
 * Content :处理pdf文件
 */
@Service
public class PdfFilePreviewImpl implements FilePreview {

    private final FileUtils fileUtils;

    private final PdfUtils pdfUtils;

    private final DownloadUtils downloadUtils;

    private static final String FILE_DIR = ConfigConstants.getFileDir();

    public PdfFilePreviewImpl(FileUtils fileUtils,
                              PdfUtils pdfUtils,
                              DownloadUtils downloadUtils) {
        this.fileUtils = fileUtils;
        this.pdfUtils = pdfUtils;
        this.downloadUtils = downloadUtils;
    }

    @Override
    public String filePreviewHandle(String url,
                                    Model model,
                                    FileAttribute fileAttribute) {
        String suffix=fileAttribute.getSuffix(); // 文件后缀
        String fileName=fileAttribute.getName(); // 文件名称
        String officePreviewType = model.asMap().get("officePreviewType") == null ?
                ConfigConstants.getOfficePreviewType() : model.asMap().get("officePreviewType").toString();
        String baseUrl = BaseUrlFilter.getBaseUrl(); // 提供预览服务的地址
        String pdfName = fileName.substring(0, fileName.lastIndexOf(".") + 1) + "pdf"; //转换后的文件名称
        String outFilePath = FILE_DIR + pdfName; // 文件转换后的输出路径 例: D:\kkFileview\test.xlsx

        // 当展示类型为 image或allImages 时,进行文件下载,类型转换
        if (OfficeFilePreviewImpl.OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType) ||
                OfficeFilePreviewImpl.OFFICE_PREVIEW_TYPE_ALL_IMAGES.equals(officePreviewType)) {

            // 1.当缓存中的文件不存在 或者 禁用缓存时，就去下载文件
            if (!fileUtils.listConvertedFiles().containsKey(pdfName) || !ConfigConstants.isCacheEnabled()) {
                // 下载远程端文件
                ReturnResponse<String> response = downloadUtils.downLoad(fileAttribute, fileName);
                if (0 != response.getCode()) {
                    model.addAttribute("fileType", suffix);
                    model.addAttribute("msg", response.getMsg());
                    return "fileNotSupported";
                }

                // 远程端文件下载到kkfile服务器后的地址
                outFilePath = response.getContent();
                if (ConfigConstants.isCacheEnabled()) {
                    // 加入缓存
                    fileUtils.addConvertedFile(pdfName, fileUtils.getRelativePath(outFilePath));
                }
            }

            // 2. 文件类型转换
            List<String> imageUrls = pdfUtils.pdf2jpg(outFilePath, pdfName, baseUrl);
            if (imageUrls == null || imageUrls.size() < 1) {
                model.addAttribute("msg", "pdf转图片异常，请联系管理员");
                model.addAttribute("fileType",fileAttribute.getSuffix());
                return "fileNotSupported";
            }
            model.addAttribute("imgurls", imageUrls);
            model.addAttribute("currentUrl", imageUrls.get(0));

            // 对应页面跳转
            if (OfficeFilePreviewImpl.OFFICE_PREVIEW_TYPE_IMAGE.equals(officePreviewType)) {
                return "officePicture";
            } else {
                return "picture";
            }
        } else {
            // 不是http开头，浏览器不能直接访问，需下载到本地
            if (url != null && !url.toLowerCase().startsWith("http")) {
                if (!fileUtils.listConvertedFiles().containsKey(pdfName) || !ConfigConstants.isCacheEnabled()) {
                    ReturnResponse<String> response = downloadUtils.downLoad(fileAttribute, pdfName);
                    if (0 != response.getCode()) {
                        model.addAttribute("fileType", suffix);
                        model.addAttribute("msg", response.getMsg());
                        return "fileNotSupported";
                    }
                    model.addAttribute("pdfUrl", fileUtils.getRelativePath(response.getContent()));
                    if (ConfigConstants.isCacheEnabled()) {
                        // 加入缓存
                        fileUtils.addConvertedFile(pdfName, fileUtils.getRelativePath(outFilePath));
                    }
                } else {
                    model.addAttribute("pdfUrl", pdfName);
                }
            } else {
                model.addAttribute("pdfUrl", url);
            }
        }
        return "pdf";
    }
}
